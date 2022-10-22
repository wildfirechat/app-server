# Linux Service 方式运行
除了命令行方式直接执行APP服务外，还可以以linux systemd service方式来运行，注意以这种方式运行，APP服务的配置还是需要按照常规方法来配置。

## 获取软件包
下载野火release或则会自己源码编译，得到Jar包```app-${version}.jar```。

## 部署软件包
创建```/usr/local/app-server```目录，把Jar包```app-${version}.jar```改名为```app-server.jar```；把config目录也拷贝到```/usr/local/app-server```目录下。

## 放置Server File
把```app-server.service```放到```/usr/lib/systemd/system/```目录下。

## 管理服务
* 刷新配置，当安装或者更新后需要执行： ```sudo systemctl daemon-reload```
* 启动服务： ```sudo systemctl start app-server.service```
* 停止服务： ```sudo systemctl stop app-server.service```
* 重启服务： ```sudo systemctl restart app-server.service```
* 查看服务状态：```sudo systemctl status app-server.service```
* 设置开机自启动：```sudo systemctl enable app-server.service```
* 禁止开机自启动：```sudo systemctl disable app-server.service```
* 查看控制台日志: ```journalctl -f -u app-server.service```

## 日志
日志主要看制台日志。如果需要看日志，请使用命令```journalctl -f -u app-server.service```来查看日志。

## 配置
需要对APP服务配置来达到最好的执行效果，配置文件在````/usr/local/app-server/config````目录下。另外还可以设置服务的内存大小，修改```/usr/lib/systemd/system/app-server.service```文件，在java命令中添加```-Xmx```参数。
