# 野火IM后端应用
作为野火IM的后端应用的演示，本工程具有如下功能：
1. 短信登陆和注册功能，用来演示登陆应用，获取token的场景.
2. PC端扫码登录的功能.
3. 群公告的获取和更新功能.
4. 客户端上传日志功能.
> 本工程为Demo工程，实际使用时需要把对应功能移植到您的应用服务中。如果需要直接使用，请按照后面的说明解决掉性能瓶颈问题。

#### 编译
```
mvn package
```

#### 短信资源
应用使用的是腾讯云短信功能，需要申请到```appid/appkey/templateId```这三个参数，并配置到```tencent_sms.properties```中去。用户也可以自行更换为自己喜欢的短信提供商。在没有短信供应商的情况下，为了测试可以使用```superCode```，设置好后，客户端可以直接使用```superCode```进行登陆。上线时一定要注意删掉```superCode```。

#### 修改配置
本演示服务有4个配置文件在工程的```config```目录下，分别是```application.properties```, ```im.properties```, ```aliyun_sms.properties```和```tencent_sms.properties```。请正确配置放到jar包所在的目录下的```config```目录下。
> ```application.properties```配置中的```sms.verdor```决定是使用那个短信服务商，1为腾讯短信，2为阿里云短信

#### 运行
在```target```目录找到```app-XXXX.jar```，把jar包和放置配置文件的```config```目录放到一起，然后执行下面命令：
```
java -jar app-XXXXX.jar
```

#### 性能瓶颈
本服务最早只提供获取token功能，后来逐渐增加了群公告/Shiro等功能，需要引入数据库。为了提高用户体验的便利性，引入了数据库[H2](http://www.h2database.com)，让用户可以无需安装任何软件就可以直接运行（JRE还是需要的），另外shiro的session也存储在h2数据库中。提高了便利性的同时导致一方面性能有瓶颈，另外一方面也不能水平扩展和高可用。因此需要使用本工程上线必须修改2个地方。
1. 切换到MySQL，切换方法请参考 ```application.properties``` 文件中的描述。
2. 使用RedisSessionDao，详情请参考 https://www.baidu.com/s?wd=shiro+redis&tn=84053098_3_dg&ie=utf-8

#### 版本兼容
+ 0.40版本引入了shiro功能，在升级本服务之前，需要确保客户端已经引入了本工程0.40版本发布时或之后的移动客户端。并且在升级之后，客户端需要退出重新登录一次以便保存session(退出登录时调用disconnect，需要使用false值，这样重新登录才能保留历史聊天记录，一定要在新版本中改成这样)。如果是旧版本或者没有重新登录，群公告和扫码登录功能将不可用。为了系统的安全性，建议升级。

+ 0.43版本把Web和PC登录的短轮询改为长轮询，如果应用服务升级需要对Web和PC进行对应修改。

#### 注意事项
服务中对同一个IP的请求会有限频，默认是一个ip一小时可以请求200次，可以根据您的实际情况调整（搜索rateLimiter字符串就能找到）。如果使用了nginx做反向代理需要注意把用户真实ip带过去，避免获取不到真实ip从而影响正常使用。

#### 使用到的开源代码
1. [TypeBuilder](https://github.com/ikidou/TypeBuilder) 一个用于生成泛型的简易Builder

#### LICENSE
UNDER MIT LICENSE. 详情见LICENSE文件


#### 使用阿里云短信
请参考说明[使用阿里云短信](./aliyun_sms.md)
