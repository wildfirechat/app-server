# Shiro Session 持久化与写库说明

> 适用范围：当前工作区代码（app-0.74）
> 相关类：`ShiroSessionManager`、`DBSessionDao`、`ShiroSessionRepository`、`ShiroSession`、`ShiroConfig`、`Application`
> 行号以当前工作区为准，后续代码调整后请以实际代码为准。

## 1. 结论

- Shiro 的 session 存在数据库表 `shiro_session` 中，`session_data` 是 Java 序列化后的 `SimpleSession`。
- 原生 Shiro 的行为是：**每个带有效 authToken 的请求都会 touch session，最终调用 `SessionDAO.update()`**，也就是每个请求写一次库。
- 当前代码已经做了两层优化：
  1. `ShiroSessionManager.touch()` 做 **30 分钟节流**；
  2. 会话永不过期时，touch **只更新 `update_time`**，不再序列化、不再重写 `session_data`。
- 结果是：活跃会话的写库频率从“每请求一次全量写”降到“**约每 30 分钟一次轻量 UPDATE**”（请求稀疏时会更久）；`session_data` 只在创建、登录、属性变更、退出等场景写。

## 2. 数据模型

`shiro_session` 表由 `ShiroSession` 实体映射：

| 列 | 类型 | 说明 |
|---|---|---|
| `session_id` | varchar(128)，主键 | Shiro session id，也是客户端拿到的 authToken |
| `session_data` | BLOB/LOB（映射声明 length=2048） | `SimpleSession` 的 Java 序列化结果，包含 id、时间戳、timeout、attributes 等 |
| `update_time` | bigint | 最后一次写库时间（毫秒），只用于清理任务判断会话是否活跃 |

两个本地缓存都在 `DBSessionDao` 里，TTL 60 分钟：

- `sessionReadCache`：`sessionId -> Session`，避免同一 session 频繁读库；
- `sessionDataCache`：`sessionId -> byte[]`，保存最后一次全量写入的序列化字节，用于 `update()` 的脏检查。

> 缓存是**每个节点各自一份**，不是分布式缓存；多节点的一致性问题见第 8 节。

## 3. 一次带 authToken 的请求发生了什么

```text
Client（header: authToken）
  │
  ▼
AbstractShiroFilter.doFilterInternal
  ├─ createSubject
  │    └─ DefaultSecurityManager.resolveSession
  │         └─ DBSessionDao.readSession
  │              ├─ 本地读缓存命中 → 直接返回 Session
  │              └─ 未命中 → SELECT shiro_session
  │                           → 反序列化 session_data
  │                           → 回填 sessionReadCache / sessionDataCache
  │                           → UPDATE shiro_session SET update_time=?
  │
  ├─ updateSessionLastAccessTime
  │    └─ subject.getSession(false).touch()
  │         └─ DelegatingSession.touch()
  │              └─ ShiroSessionManager.touch(key)
  │                   ├─ 距上次写库 < 30 分钟 → return（0 次写库）
  │                   ├─ 会话永不过期 → session.touch()
  │                   │                  + UPDATE shiro_session SET update_time=?
  │                   └─ 超时是有限值 → session.touch()
  │                                      + DBSessionDao.update()（全量写 session_data）
  │
  └─ 业务 Filter / Controller
```

关键点：

1. `authToken` 在 `ShiroSessionManager.getSessionId()` 里被直接当作 sessionId。
2. `updateSessionLastAccessTime()` 在 Shiro 的 filter chain **之前**执行，所以 `anon` 接口只要带了有效 authToken，也会走 touch。
3. `authToken` 无效、查不到 session 时，`subject.getSession(false)` 为 `null`，不会 touch，也不会写库。
4. `ShiroSessionManager.touch()` 内部会再 `retrieveSession(key)` 一次，通常命中 `sessionReadCache`，不会产生额外 SQL。
5. 没有 authToken 时会回退到 cookie 里的 sessionId；只要解析出有效 session，同样会走 touch。

## 4. Session 在生命周期中会被修改什么

| 时机 | 变化内容 | 是否写库 | 走的路径 |
|---|---|---|---|
| 创建 session | id、host、startTimestamp、timeout | 是，INSERT | `create()` 里直接调用 `update()` |
| 登录 | 业务属性 `userId` | 是，全量写 | `ServiceImpl` 的 `setAttribute("userId", ...)` |
| 登录 | Shiro 的 principals、authenticated | 登录时写入；后续请求比较相同则跳过 | `DefaultSubjectDAO.saveToSession()` |
| 每次请求 | 只有 `lastAccessTime` | 最多每 30 分钟一次轻量 UPDATE | `ShiroSessionManager.touch()` |
| PC 扫码确认 | `userId` | 是，全量写 | `ServiceImpl` 的 `setAttribute("userId", ...)` |
| 退出 / 销毁 | stopTimestamp、lastAccessTime | 先全量写，再 DELETE | `stop()` → `onStop` → `delete()` |
| 超时 | expired | 不会发生 | `globalSessionTimeout = Long.MAX_VALUE` |
| 清理任务 | 删除 90 天未更新的行 | 每小时一次 DELETE | `Application.clearExpiredShiroSession()` |

业务代码里对 Shiro session 的 `setAttribute` 只有两处，都是登录流程：

- `ServiceImpl.java:847`：密码/验证码登录后写 `userId`；
- `ServiceImpl.java:1167`：PC 扫码确认后写 `userId`。

登录完成后，运行期间**没有代码修改 session 的业务属性**，每次请求唯一的变化就是 `lastAccessTime`。这是“touch 只更新 update_time”成立的前提。

## 5. 当前写库策略

### 5.1 touch 节流

```java
// ShiroSessionManager.java:29
private static final long TOUCH_INTERVAL_MILLIS = 30 * 60 * 1000L;
```

- `touch()` 取 session 的 `lastAccessTime`，距现在不足 30 分钟就直接返回；
- 只有超过 30 分钟才 `session.touch()` 并写库；
- 因为 `lastAccessTime` 只在真正写库时更新，实际写入间隔**最短 30 分钟**；请求越稀疏，间隔越长，不会固定在 30 分钟。

### 5.2 轻量更新 vs 全量更新

`ShiroSessionManager.touch()` 在 `session.touch()` 之后判断：

```java
long timeout = session.getTimeout();
boolean neverExpires = timeout < 0 || timeout == Long.MAX_VALUE;
if (neverExpires && getSessionDAO() instanceof DBSessionDao) {
    // 只更新 update_time
    ((DBSessionDao) getSessionDAO()).updateLastAccessTime(session.getId());
    return;
}
// 有限超时：退回原来的全量更新
onChange(session);
```

- **永不过期**（`timeout < 0` 或 `Long.MAX_VALUE`）：只执行
  ```sql
  UPDATE shiro_session SET update_time = ? WHERE session_id = ?;
  ```
  不序列化、不 SELECT、不写 `session_data`。
- **有限超时**：仍走 `DBSessionDao.update()` 全量写，避免重启或读缓存加载到过旧的 `lastAccessTime` 而误判过期。

轻量更新不会刷新 `sessionDataCache` 里的字节快照（里面还是旧的 `lastAccessTime`）；之后如果发生真正的属性变更，`update()` 会正常全量写并刷新快照，这是预期行为。

`DBSessionDao.updateLastAccessTime()` 的实现只有一行：

```java
public void updateLastAccessTime(Serializable sessionId) {
    shiroSessionRepository.updateAccessTime((String) sessionId, System.currentTimeMillis());
}
```

### 5.3 全量更新 `update()`

```java
byte[] currentBytes = sessionToByte(session);   // Java 序列化整个 SimpleSession
byte[] cachedBytes = sessionDataCache.getIfPresent(sessionId);
if (cachedBytes != null && Arrays.equals(cachedBytes, currentBytes)) {
    return;                                     // 脏检查：完全没变则跳过
}
ShiroSession shiroSession = new ShiroSession((String) sessionId, currentBytes);
shiroSession.setUpdateTime(System.currentTimeMillis());
shiroSessionRepository.save(shiroSession);
```

说明：

- `session_data` 里包含 `lastAccessTime`，所以只要 touch 走到 `update()`，序列化字节必然变化，**脏检查在 touch 路径上不会命中**；
- 脏检查主要挡住“状态完全相同的重复全量更新”（例如创建后重复设置相同的 timeout）；
- `save()` 对已有主键走 Hibernate 的 `merge`，通常是一条 `SELECT` + 一条 `UPDATE`；新建时是 `SELECT` + `INSERT`。

### 5.4 读缓存未命中

`readSession()` 在本地读缓存未命中时会：

1. `SELECT` 出 `shiro_session`；
2. 反序列化 `session_data`；
3. 回填 `sessionReadCache` 和 `sessionDataCache`；
4. 额外执行一次 `UPDATE update_time`。

第 4 步是读缓存过期后的保活刷新，每个活跃会话大约 **每小时每节点一次**。

### 5.5 超时兜底

- `ShiroConfig` 里 `globalSessionTimeout = Long.MAX_VALUE`，会话不会因为空闲被 Shiro 主动过期；
- 真正的“过期”由数据库清理任务按 `update_time < now - 90 天` 删除；
- 如果以后把超时改成有限值：
  - touch 会自动退回全量更新；
  - 必须保证超时时间 **大于 30 分钟**，否则会话可能在两次 touch 之间就被判过期。

## 6. 写库频率估算

设在线会话数为 `U`，节点数为 `N`：

```text
touch 写库 QPS        ≈ U / 1800 × N
读缓存未命中写库 QPS  ≈ U / 3600 × N
```

以单节点、1 万在线为例：

| 操作 | 改前（60 秒全量） | 改后（30 分钟轻量） |
|---|---|---|
| touch | 约 167 次/秒，每次序列化 + SELECT + UPDATE | 约 5.6 次/秒，单条 UPDATE |
| 读缓存未命中 | 约 2.8 次/秒 | 约 2.8 次/秒 |
| 稳态写库合计 | 约 170 次/秒全量写，另加约 167 次/秒 SELECT | 约 8.4 次/秒 UPDATE |

多节点按 `N` 放大（最坏情况，假设每个节点都会服务到该会话）：

| 在线数 | 节点数 | 改前 touch 全量写 | 改后 touch 轻量写 |
|---|---|---|---|
| 1 万 | 1 | 约 167 次/秒 | 约 5.6 次/秒 |
| 1 万 | 3 | 约 500 次/秒 | 约 17 次/秒 |
| 10 万 | 3 | 约 5000 次/秒 | 约 167 次/秒 |

> 以上是“每个会话每 30 分钟至少被访问一次”的上界。用户空闲时不会产生 touch 写入。
> 实际值还受请求分布、LB 粘性、缓存命中率影响。

## 7. 正确性前提

这套优化依赖以下前提，改动相关配置前请先读本节：

1. **`globalSessionTimeout` 保持 `Long.MAX_VALUE`**
   Shiro 只有在 timeout 是有限值时才用 `lastAccessTime` 判断过期。本项目是永不过期，所以 `session_data` 里的 `lastAccessTime` 旧一些没有影响。
2. **如果改成有限超时**
   - touch 会自动走全量更新；
   - 超时时间必须大于 30 分钟；
   - 多节点时钟偏差也要留出余量。
3. **`update_time` 的语义**
   它是“最后一次写库/保活时间”，只用于 90 天清理，不是登录有效期。更新它不会延长或缩短登录状态。
4. **`session_data` 里的 `lastAccessTime` 会变旧**
   它只在上一次全量写时更新。会话永不过期时这没有影响；如果外部有报表/脚本读这个字段，应改用 `update_time`。
5. **登录/属性变更仍然实时写**
   `setAttribute` 会立即触发 `onChange` → 全量 `update()`，不受 touch 节流影响，所以 `userId` 不会丢。
6. **Shiro 的定时校验不会遍历数据库 session**
   `getActiveSessions()` 返回空集合，Shiro 的 `SessionValidationScheduler` 实际上不会对数据库 session 做批量校验；会话清理完全依赖 `update_time` 的 SQL 任务。

## 8. 多节点部署

### 8.1 这个改动本身

- session 在共享 MySQL 中，任何节点都能读；
- touch 节流是**每节点本地**的，不是全局节流；
- 所以同一个会话如果轮流落到 `N` 个节点，最坏是 `N` 个节点各自最多每 30 分钟写一次，写库量按 `N` 放大；
- `update_time` 仍然远小于 90 天阈值，清理不会误删；
- 滚动升级兼容：旧节点继续全量写，新节点轻量写，两者操作同一行，不需要改表结构。

### 8.2 已有的多节点问题（与本次改动无关）

1. **本地读缓存 60 分钟**
   A 节点 `setAttribute` 后，B 节点最多 60 分钟看不到新值。
2. **登出跨节点可见性**
   A 节点 `logout()` 会删掉数据库行，但 B 节点的 `sessionReadCache` 还在，最多 60 分钟内仍可能认这个 authToken。
   本次改动让 B 的 touch 只执行 `UPDATE update_time`，行不存在时影响 0 行，**不会再把已删除的 session 重新 INSERT 回来**；但 B 仍然会从缓存里放行请求。
3. **清理任务每个节点都会跑**
   `@Scheduled` 在每个实例上都会执行，多个节点会并发 DELETE。虽然条件幂等，但数据量大时可能有锁等待。建议只让一个实例执行清理，或加分布式锁/开关。
4. **时钟偏差**
   30 分钟 vs 90 天，正常 NTP 偏差没有影响；如果改成有限超时，需要把偏差算进去。

### 8.3 建议

- **LB 按 authToken/用户粘性**：同一会话固定落到同一节点，上述缓存问题基本不会发生，当前方案可以直接用。
- **LB 不粘性**：
  - 当前方案仍然安全，只是写库量按节点数放大；
  - 如果在意跨节点一致性，优先考虑 README 推荐的 `RedisSessionDao`（共享 session，读到即最新）；
  - 或者缩短 `sessionReadCache` TTL、增加跨节点失效通知；
  - 不要把“30 分钟 touch”当成多节点一致性的解决方案，它只解决写放大。

## 9. 运维与验证

### 9.1 观察 `update_time`

```sql
SELECT session_id,
       FROM_UNIXTIME(update_time / 1000) AS last_write,
       LENGTH(session_data) AS data_size
FROM shiro_session
ORDER BY update_time DESC
LIMIT 20;
```

预期：

- 活跃会话的 `update_time` 至少每 30 分钟才可能变化一次，通常约 30~60 分钟；请求越稀疏，间隔越长；
- `data_size` 通常只有几百字节（`session_data` 只是 userId、principals、认证标记等少量数据）；
- 不再出现“每秒都在变”的情况。

### 9.2 统计写入量

```sql
SHOW GLOBAL STATUS LIKE 'Com_update';
```

间隔 60 秒取两次，算增量。注意 `Com_update` 包含全库所有 UPDATE，要看 `shiro_session` 的精确值，建议短时间开 general log 或用 `performance_schema`：

```sql
-- 谨慎：只在排查时短时间开启
SET GLOBAL general_log = 'ON';
-- 观察后关闭
SET GLOBAL general_log = 'OFF';
```

### 9.3 上线检查清单

- [ ] 登录后连续请求，`shiro_session.update_time` 在 30 分钟内不再频繁变化；
- [ ] 登录后 `userId` 已写入 `session_data`（或至少接口能正常拿到用户信息）；
- [ ] 重启应用后，原 authToken 仍能正常访问；
- [ ] 退出登录后，当前节点不再接受该 authToken；
- [ ] 多节点部署时，分别直连每个节点验证 authToken 可用；
- [ ] 观察 30 分钟后，`update_time` 有正常刷新；
- [ ] 数据量大时，确认清理任务执行时间可接受。

### 9.4 测试环境验证 30 分钟节流

生产上等 30 分钟不方便，可以在测试环境把 `TOUCH_INTERVAL_MILLIS` 临时改成 30 秒，重新编译后：

1. 登录；
2. 每秒请求一次，观察 `update_time` 是否约 30 秒才变一次；
3. 确认业务属性（`userId`）只在登录时写，不会被 touch 覆盖。

验证完记得改回 30 分钟。

## 10. 升级与回滚

- **表结构**：无变化，不需要迁移。
- **配置**：无新增配置项，`globalSessionTimeout` 保持 `Long.MAX_VALUE` 即可。
- **构建**：`TOUCH_INTERVAL_MILLIS` 是 `static final` 编译期常量，必须重新编译打包，改配置/热更新不生效。
- **滚动升级**：新旧节点可以共存；旧节点全量写、新节点轻量写，互不冲突。
- **回滚**：旧代码读取 `session_data` 后，会在下一次 touch 时全量写回；`lastAccessTime` 可能旧一些，但 timeout 是 `Long.MAX_VALUE`，不会因此过期。

## 11. 后续可选优化

1. **RedisSessionDao**
   多节点、大规模场景的彻底方案，也是 README 中推荐的方案。session 集中存储后，不需要本地 60 分钟读缓存，跨节点一致性更好。
2. **缩短读缓存 TTL / 增加失效通知**
   如果暂时不换 Redis，但需要更快看到属性变更和登出，可以缩短 `CACHE_TTL_MINUTES`，或增加跨节点失效通知。注意：缩短 TTL 会增加 `readSession` 未命中和 `update_time` 刷新次数，需要一起评估。
3. **给 `update_time` 加索引**
   当前实体没有声明索引，清理 SQL 会全表扫描。`shiro_session` 很大时可以加索引，但会增加每次 UPDATE 的索引维护成本，需权衡。
4. **去掉 `readSession` 未命中时的 `update_time` 刷新**
   如果确认 touch 已经能保证活跃会话的 `update_time` 新鲜，这一步可以省掉；但它是缓存 TTL 变化时的安全网，建议保留。
5. **给缓存设置 `maximumSize`**
   当前两个 Guava cache 只有 TTL，没有容量上限；超大规模下可以加上限，避免内存随会话数增长。

## 12. FAQ

**Q1：为什么每个带 authToken 的请求都会走到 touch？**

因为 `AbstractShiroFilter` 在进入业务 filter chain 之前会执行 `updateSessionLastAccessTime()`，调用 `subject.getSession(false).touch()`。这是 Shiro 的固定行为，和具体接口是否 `anon` 无关。

**Q2：touch 路径上的脏检查为什么没挡住写库？**

`SimpleSession` 的 `lastAccessTime` 也参与 Java 序列化。`session.touch()` 刚改完时间，序列化字节必然不同，所以只要走到 `update()` 就会写。脏检查只能挡住“状态完全相同的重复全量更新”。

**Q3：更新 `update_time` 会延长登录有效期吗？**

不会。Shiro 的过期判断看 `lastAccessTime + timeout`，而本项目 timeout 是 `Long.MAX_VALUE`，本来就不会过期。`update_time` 只影响 90 天清理任务。

**Q4：会话 90 天不用会被删除吗？**

会。`Application.clearExpiredShiroSession()` 每小时删除 `update_time < now - 90 天` 的行。活跃会话的 `update_time` 会被 touch 或读缓存未命中刷新，不会被误删。

**Q5：为什么不直接用 `@DynamicUpdate` 只更新变化的列？**

`update()` 每次都会把 `session_data` 显式设置为新序列化的字节数组，Hibernate 会认为该列发生了变化；`@DynamicUpdate` 解决不了“每次 touch 都重新序列化”的根本问题，而且 LOB 列仍然会被写。

**Q6：重启后 session 会丢吗？**

不会。`create()` 在创建时就 `INSERT` 到数据库；启动后 `readSession()` 从 `shiro_session` 读取并反序列化。`session_data` 的 `lastAccessTime` 可能旧一些，但 timeout 是 `Long.MAX_VALUE`，不会因此过期。

**Q7：为什么 `session_data` 里的 `lastAccessTime` 不跟着 touch 更新？**

为了省掉每次 touch 的序列化和 LOB 重写。这个字段在当前项目里不参与过期判断，真正用于保活的是 `update_time`。如果以后把 timeout 改成有限值，touch 会自动退回全量更新。

## 13. 代码索引

| 位置 | 作用 |
|---|---|
| `ShiroSessionManager.getSessionId()` | 从 `authToken` 请求头取 sessionId |
| `ShiroSessionManager.touch()` | 30 分钟节流；永不过期时只刷新 `update_time` |
| `DBSessionDao.create()` | 创建 session 并立即入库 |
| `DBSessionDao.readSession()` | 读缓存 / 读库 / 回填缓存 / 刷新 `update_time` |
| `DBSessionDao.update()` | 全量写 `session_data` + `update_time`，带脏检查 |
| `DBSessionDao.updateLastAccessTime()` | 只更新 `update_time` |
| `DBSessionDao.delete()` | 删除 session 并清理本地缓存 |
| `ShiroSessionRepository.updateAccessTime()` | 轻量 UPDATE |
| `ShiroSessionRepository.deleteExpiredSessions()` | 删除 90 天未更新的 session |
| `ShiroConfig.securityManager()` | 设置 `globalSessionTimeout = Long.MAX_VALUE` |
| `Application.clearExpiredShiroSession()` | 每小时执行一次清理 |
