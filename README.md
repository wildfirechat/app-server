# 野火IM后端应用
作为野火IM的后端应用的演示，本工程仅具备短信登陆功能，用来演示获取登陆应用，获取token的场景。

#### 编译
```
mvn package
```

#### 短信资源
应用使用的是腾讯云短信功能，需要申请到```appid/appkey/templateId```这三个参数，并配置到```sms.properties```中去。用户也可以自行更换为自己喜欢的短信提供商。

#### 修改配置
本演示服务有3个配置文件，分别是```application.properties```, ```im.properties```和```sms.properties```。请直接在工程的resource目录下修改打包进工程。或者放到jar包所在的目录下的```config```目录下。

#### 运行
```
nohup java -jar app-XXXXX.jar > app.log 2>&1 &
```

#### 鸣谢
1. [TypeBuilder](https://github.com/ikidou/TypeBuilder) 一个用于生成泛型的简易Builder

#### LICENSE
UNDER MIT LICENSE. 详情见LICENSE文件
