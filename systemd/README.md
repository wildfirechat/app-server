# Linux Service 方式运行
除了命令行方式直接执行APP服务外，还可以以linux systemd service方式来运行，注意以这种方式运行，APP服务的配置还是需要按照常规方法来配置。

## 获取软件包
下载野火release或则会自己源码编译，得到```app-${version}.jar```、```app-${version}.deb```和```app-${version}.rpm```。

## 手动部署
### 依赖
野火IM依赖JRE1.8手动部署需要手动安装JRE1.8，确保命令:```java -version```能看到正确的java版本信息才行。

### 部署软件包
创建```/opt/app-server```目录，把Jar包```app-${version}.jar```改名为```app-server.jar```；把config目录也拷贝到```/opt/app-server```目录下。

### 放置systemd server file
把```app-server.service```放到```/usr/lib/systemd/system/```目录下。

### 测试
根据下面管理服务的说明，启动服务，查看控制台日志，确认启动没有异常，服务器本地执行 ```curl -v http://127.0.0.1:8888``` 能够返回字符串```Ok```。

## 安装部署
### 依赖
安装包安装将会自动安装依赖，不需要手动安装java。如果服务器上有其他版本的Java，请注意可能的冲突问题。

### 部署软件包
可以直接安装```deb```和```rpm```格式的安装包，在debian系的linux系统（Ubuntu等使用```apt```命令安装软件的系统）中，使用命令：
```shell
sudo apt install ./app-server-{version}.deb
```

在红帽系的linux系统（Centos等使用```yum```命令安装软件的系统）中，使用命令:
```shell
sudo yum install ./app-server-${version}.deb
```

注意在上述两个命令中，都使用的是本地安装，注意安装包名前的```./```路径。如果使用```dpkg -i ./app-server-${version}.deb```命令将不会安装依赖。

### 测试
根据下面管理服务的说明，启动服务，查看控制台日志，确认启动没有异常，服务器本地执行 ```curl -v http://127.0.0.1:8888``` 能够返回字符串```Ok```。


## 管理服务
* 刷新配置，当安装或者更新后需要执行： ```sudo systemctl daemon-reload```
* 启动服务： ```sudo systemctl start app-server```
* 停止服务： ```sudo systemctl stop app-server```
* 重启服务： ```sudo systemctl restart app-server```
* 查看服务状态：```sudo systemctl status app-server```
* 设置开机自启动：```sudo systemctl enable app-server```
* 禁止开机自启动：```sudo systemctl disable app-server```
* 查看控制台日志: ```journalctl -f -u app-server```

## 日志
日志主要看制台日志。如果需要看日志，请使用命令```journalctl -f -u app-server```来查看日志。

## 配置
需要对APP服务配置来达到最好的执行效果，配置文件在````/opt/app-server/config````目录下。另外还可以设置服务的内存大小，修改```/usr/lib/systemd/system/app-server```文件，在java命令中添加```-Xmx```参数。
