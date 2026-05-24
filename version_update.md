# 客户端版本更新配置说明

本文档说明如何在应用服务端配置客户端版本更新提示功能。

## 功能概述

应用服务端支持为 **Android / iOS / 鸿蒙(HarmonyOS)** 三端客户端提供版本更新检查能力：

- 服务端通过 `config/version.properties` 文件管理各平台版本信息
- 服务每 **30秒** 自动检测该文件是否有修改，如有修改则自动重新加载
- 客户端启动后自动调用 `/version/check` 接口检测版本
- 支持 **普通更新**（用户可选择跳过）和 **强制更新**（阻断使用）两种策略

## 配置文件

配置文件路径：`config/version.properties`

### 文件格式

```properties
# ============================================
# Android 版本配置
# ============================================
version.android.latestVersion=1.3.0
version.android.buildNumber=130
version.android.minVersion=1.0.0
version.android.forceUpdate=false
version.android.title=发现新版本
version.android.message=1. 优化性能\n2. 修复已知问题\n3. 提升稳定性
version.android.downloadUrl=https://example.com/app.apk

# ============================================
# iOS 版本配置
# ============================================
version.ios.latestVersion=1.3.0
version.ios.buildNumber=130
version.ios.minVersion=1.0.0
version.ios.forceUpdate=false
version.ios.title=发现新版本
version.ios.message=1. 优化性能\n2. 修复已知问题\n3. 提升稳定性
version.ios.redirectUrl=itms-apps://itunes.apple.com/app/id1234567890

# ============================================
# 鸿蒙 HarmonyOS 版本配置
# ============================================
version.harmony.latestVersion=1.3.0
version.harmony.buildNumber=130
version.harmony.minVersion=1.0.0
version.harmony.forceUpdate=false
version.harmony.title=发现新版本
version.harmony.message=1. 优化性能\n2. 修复已知问题\n3. 提升稳定性
version.harmony.harmonyUrl=https://appgallery.huawei.com/app/detail?id=xxx
```

### 字段说明

| 字段 | 必填 | 说明 |
|------|------|------|
| `latestVersion` | 是 | 最新版本号，如 `1.3.0`，用于展示和版本比较 |
| `buildNumber` | 是 | 最新构建号，如 `130`，**优先级高于版本号**，用于精确版本比较 |
| `minVersion` | 是 | 最低可用版本，如 `1.0.0`。客户端版本低于此值时自动触发强制更新 |
| `forceUpdate` | 是 | 是否强制更新。`true`=所有低于最新版的客户端都必须更新；`false`=允许跳过 |
| `title` | 是 | 更新弹窗标题 |
| `message` | 是 | 更新内容详情，支持 `\n` 换行 |
| `downloadUrl` | Android必填 | Android APK 直链下载地址 |
| `redirectUrl` | iOS必填 | iOS App Store 跳转链接，支持 `itms-apps://` 协议 |
| `harmonyUrl` | 鸿蒙必填 | 鸿蒙应用市场跳转链接 |

### 平台标识说明

| 平台 | properties前缀 | 请求platform参数 |
|------|----------------|------------------|
| Android | `version.android.*` | `2` |
| iOS | `version.ios.*` | `1` |
| 鸿蒙 HarmonyOS | `version.harmony.*` | `10` |

## 版本比较逻辑

服务端按以下规则判断是否需要更新：

### 1. 优先使用 buildNumber 比较（推荐）

如果客户端和服务端都提供了 **大于0** 的 buildNumber，直接比较数字大小：

```
客户端 buildNumber < 服务端 buildNumber  → 需要更新
```

**优点**：避免 `1.10.0` 字符串比较时被误判为小于 `1.2.0` 的问题。

### 2. 备用：语义化版本号比较

如果 buildNumber 为 0 或未提供，使用版本号字符串比较：

```
"1.2.0" < "1.3.0"  → 需要更新
"1.10.0" > "1.3.0" → 不需要更新（按数字逐位比较）
```

## 强制更新策略

强制更新由两个条件共同决定，**满足任一即强制**：

| 条件 | 优先级 | 说明 |
|------|--------|------|
| `forceUpdate=true` | 高 | 服务端显式开启强制更新，所有低于最新版的客户端都必须更新 |
| `currentVersion < minVersion` | 兜底 | 客户端当前版本低于最低可用版本时自动强制更新 |

**使用建议**：
- 日常发版 → `forceUpdate=false`，设置 `minVersion` 为最低兼容版本
- 紧急事故（如发现严重安全漏洞）→ 临时改为 `forceUpdate=true`，秒级生效

## 接口说明

### 请求

```
GET /version/check?platform={platform}&currentVersion={version}&buildNumber={buildNumber}
```

**参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| platform | int | 是 | 平台标识：1=iOS, 2=Android, 10=鸿蒙 |
| currentVersion | string | 是 | 客户端当前版本号，如 `1.2.0` |
| buildNumber | int | 否 | 客户端当前构建号，默认 `0` |

### 响应

```json
{
  "code": 0,
  "message": "success",
  "result": {
    "needUpdate": true,
    "forceUpdate": false,
    "latestVersion": "1.3.0",
    "buildNumber": 130,
    "title": "发现新版本",
    "message": "1. 优化性能\n2. 修复已知问题",
    "downloadUrl": "https://example.com/app.apk",
    "redirectUrl": "https://apps.apple.com/app/id123456",
    "harmonyUrl": "https://appgallery.huawei.com/app/xxx",
    "minVersion": "1.0.0"
  }
}
```

**返回字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| needUpdate | bool | `true`=需要更新，`false`=已是最新 |
| forceUpdate | bool | `true`=强制更新（阻断式弹窗），`false`=可选更新 |
| latestVersion | string | 服务端最新版本号 |
| buildNumber | int | 服务端最新构建号 |
| title | string | 弹窗标题 |
| message | string | 更新内容 |
| downloadUrl | string | Android下载链接 |
| redirectUrl | string | iOS跳转链接 |
| harmonyUrl | string | 鸿蒙跳转链接 |
| minVersion | string | 最低可用版本 |

## 配置示例

### 场景1：普通更新（用户可跳过）

```properties
version.android.latestVersion=1.3.0
version.android.buildNumber=130
version.android.minVersion=1.0.0
version.android.forceUpdate=false
version.android.title=发现新版本
version.android.message=1. 新增夜间模式\n2. 优化聊天体验
version.android.downloadUrl=https://example.com/app_v1.3.0.apk

version.ios.latestVersion=1.3.0
version.ios.buildNumber=130
version.ios.minVersion=1.0.0
version.ios.forceUpdate=false
version.ios.title=发现新版本
version.ios.message=1. 新增夜间模式\n2. 优化聊天体验
version.ios.redirectUrl=itms-apps://itunes.apple.com/app/id1234567890

version.harmony.latestVersion=1.3.0
version.harmony.buildNumber=130
version.harmony.minVersion=1.0.0
version.harmony.forceUpdate=false
version.harmony.title=发现新版本
version.harmony.message=1. 新增夜间模式\n2. 优化聊天体验
version.harmony.harmonyUrl=https://appgallery.huawei.com/app/detail?id=com.example.app
```

效果：
- 版本低于 `1.3.0` 或 buildNumber 小于 `130` 的客户端会收到更新提示
- 用户可以点击"以后再说"跳过

### 场景2：强制更新（阻断使用）

```properties
version.android.latestVersion=1.3.0
version.android.buildNumber=130
version.android.minVersion=1.2.0
version.android.forceUpdate=true
version.android.title=重要更新
version.android.message=修复了可能导致数据丢失的严重问题，请立即更新
version.android.downloadUrl=https://example.com/app_v1.3.0.apk

version.ios.latestVersion=1.3.0
version.ios.buildNumber=130
version.ios.minVersion=1.2.0
version.ios.forceUpdate=true
version.ios.title=重要更新
version.ios.message=修复了可能导致数据丢失的严重问题，请立即更新
version.ios.redirectUrl=itms-apps://itunes.apple.com/app/id1234567890

version.harmony.latestVersion=1.3.0
version.harmony.buildNumber=130
version.harmony.minVersion=1.2.0
version.harmony.forceUpdate=true
version.harmony.title=重要更新
version.harmony.message=修复了可能导致数据丢失的严重问题，请立即更新
version.harmony.harmonyUrl=https://appgallery.huawei.com/app/detail?id=com.example.app
```

效果：
- **所有**版本低于 `1.3.0` 的客户端都会收到强制更新弹窗
- 弹窗不可关闭，只能点击"立即更新"

### 场景3：仅对旧版本强制更新

```properties
version.android.latestVersion=1.3.0
version.android.buildNumber=130
version.android.minVersion=1.1.0
version.android.forceUpdate=false
version.android.title=发现新版本
version.android.message=建议更新到最新版本
version.android.downloadUrl=https://example.com/app_v1.3.0.apk
```

效果：
- `1.2.x` 用户：收到可选更新提示
- `1.0.x` 用户：因为低于 `minVersion(1.1.0)`，自动触发强制更新

## 热更新操作步骤

修改版本配置 **无需重启服务**，30秒内自动生效：

1. 编辑 `config/version.properties`
2. 保存文件
3. 等待最多30秒，服务自动加载新配置
4. 客户端下次启动或切回前台时即可检测到新版本

如需立即生效，可重启应用服务：
```bash
# 查找并重启服务
pkill -f app-*.jar
java -jar app-XXXX.jar
```

## 常见问题

### Q1：修改配置文件后没有生效？

- 检查文件路径是否正确（相对服务运行目录的 `config/version.properties`）
- 检查文件是否有语法错误（如缺少 `=` 或存在中文冒号）
- 检查文件权限，确保服务进程有读取权限
- 查看服务日志确认是否加载成功

### Q2：buildNumber 和 version 用哪个更好？

**推荐优先使用 buildNumber**：

- buildNumber 是纯数字比较，不会有歧义
- version 字符串比较（如 `1.10.0` vs `1.2.0`）在语义化版本规则下是正确的，但部分旧版客户端可能实现不一致
- 建议客户端每次发版时同时递增 buildNumber 和 version

### Q3：forceUpdate 和 minVersion 有什么区别？

| 场景 | 使用 forceUpdate=true | 使用 minVersion |
|------|----------------------|-----------------|
| 适用范围 | 所有低于最新版的客户端 | 仅低于 minVersion 的旧版本 |
| 典型用途 | 紧急安全补丁、重大事故修复 | 日常兼容性底线控制 |
| 灵活性 | 一刀切 | 可针对不同旧版本差异化处理 |

### Q4：iOS 审核是否允许强制更新？

苹果 App Store 审核指南不建议强制阻断用户使用。实际实现中：

- iOS 端代码中强制更新弹窗虽然显示"必须更新"，但技术上用户仍可通过杀进程绕过
- 建议 iOS 的 `forceUpdate` 保持为 `false`，或仅用于极少数严重安全场景
- 更新跳转使用 `itms-apps://itunes.apple.com/app/id{APPID}` 直接打开 App Store App，比 `https://` 网页链接体验更好

### Q5：鸿蒙客户端如何跳转到应用市场？

鸿蒙端使用 `want` 能力跳转：

```typescript
import { common } from '@kit.AbilityKit';
let context = getContext(this) as common.UIAbilityContext;
let want = {
  action: 'ohos.want.action.viewData',
  uri: 'https://appgallery.huawei.com/app/detail?id=com.example.app'
};
context.startAbility(want);
```

> 链接格式：`https://appgallery.huawei.com/app/detail?id={你的鸿蒙包名}`
> 示例：`https://appgallery.huawei.com/app/detail?id=com.example.imchat`

具体实现可根据项目使用的弹窗组件自行调整。
