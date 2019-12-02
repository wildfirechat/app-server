# 阿里云短信功能说明

## 短信对接
1. 在[这里](https://usercenter.console.aliyun.com/#/manage/ak)申请阿里云***accessKeyId***和***accessSecret***
2. 开通短信服务，并申请短信签名和短信模版。注意申请短信签名和模版都是需要审核的，可以同时申请，以便节省您的时间
3. 修改```config```目录下的```aliyun_sms.properities```，填入上述四个参数。比如
    ```$xslt
    alisms.accessKeyId=LTXXXXXXXXXXXXtW
    alisms.accessSecret=4pXXXXXXXXXXXXXXXXXXXXXXXXXXXXyU
    alisms.signName=野火IM
    alisms.templateCode=SMS_170000000
    ```
4. 修改默认使用阿里云短信，在```application.properites```文件中修改```sms.vendor```为***2***
5. 运行测试。

> 上述几个参数如果不明白，可以参考[阿里云文档](https://help.aliyun.com/document_detail/55284.html?spm=a2c4e.11153987.0.0.5861aeecePRLPH)

## 迁移阿里云短信功能
指导如何把阿里云短信功能迁移到客户应用服务中
1. 引入jar包
    ```$xslt
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>aliyun-java-sdk-core</artifactId>
        <version>4.1.0</version>
    </dependency>
    ```

2. 拷贝除了```Application.java```以外的所有源码到客户应用服务器.

3. 拷贝配置文件到客户应用服务，需要注意配置文件会依赖特定的路径，请放置正确的路径