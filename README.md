# 野火IM后端应用
作为野火IM的后端应用的演示，本工程仅具备短信登陆功能，用来演示获取登陆应用，获取token的场景。

#### 编译
```
mvn package
```

#### 短信资源
应用使用的是腾讯云短信功能，需要申请到```appid/appkey/templateId```这三个参数，并配置到```tencent_sms.properties```中去。用户也可以自行更换为自己喜欢的短信提供商。在没有短信供应商的情况下，为了测试可以使用```superCode```，设置好后，客户端可以直接使用```superCode```进行登陆。上线时一定要注意删掉```superCode```。

#### 修改配置
本演示服务有3个配置文件在工程的```config```目录下，分别是```application.properties```, ```im.properties```和```tencent_sms.properties```。请正确配置放到jar包所在的目录下的```config```目录下。

#### 运行
在```target```目录找到```app-XXXX.jar```，把jar包和放置配置文件的```config```目录放到一起，然后执行下面命令：
```
java -jar app-XXXXX.jar
```

#### 使用到的开源代码
1. [TypeBuilder](https://github.com/ikidou/TypeBuilder) 一个用于生成泛型的简易Builder

#### LICENSE
UNDER MIT LICENSE. 详情见LICENSE文件
