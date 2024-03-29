# bilibili直播录播工具

[![Build](https://github.com/chocotan/bili-recorder/actions/workflows/maven-docker.yml/badge.svg)](https://github.com/chocotan/bili-recorder/actions/workflows/maven-docker.yml)

这是一个大概开发好已经能用的录播工具

## 注意
* 视频投稿以及录制弹幕的功能已经长期处于未维护的状态，目前仅有录制功能正常工作

## 打包
* 你需要Java 11和maven才能正常打包
* 运行命令 `mvn clean package -Dmaven.test.skip`
* 打包完成后会在`target`目录生成可执行jar

## 配置
* record.work-path: 工作目录，不管是h2数据库文件、日志，还是录播位置，默认值为`${user.home}/.bili`
* record.check-interval: 检查直播间状态的间隔，单位是秒，默认值为25
* server.port: 启动后的端口，默认值为30000


## json配置

* convertToMp4: 是否转换为mp4格式
* convertToMp4Delete: 转换为mp4后是否删除原flv文件
* danmuRecord: 是否录制弹幕
* saveFolder: 自定义保存路径 
* splitFileBySize：是否根据文件大小分割 
* splitFileByTime：是否根据文件时间分割
* splitFileDeleteSource：分割后是否删除原文件
* splitFileDurationInSeconds：每多少秒分割成一个文件
* splitFileSizeInM：每多少M分割成一个文件
* uploadToBili：是否上传到B站
* uploadUsername：上传的账号用户名
* uploadTitleTemplate：上传的标题模板
* uploadDescTemplate：上传的描述模板

其他功能可自行探索

## 自动上传的相关说明

B站密码登录方式目前疑似不可用，本应用切换至TV扫码接口，请在列表页扫码登录，暂不确定登录有效期

* 修改uploadToBili后只针对之后录制的视频有效，之前的录制记录不会生效
* 自动上传是每2分钟检查一次，一次上传一个投稿
* 仅支持上传12小时内结束直播的记录，如有需求请自行修改源码
* 当一个投稿上传失败时，会重试，最大重试次数为3次，超过将不处理

## 使用
* 你可以使用 `mvn clean package -Dmaven.test.skip` 命令打包后使用 java -jar 命令启动
* 默认端口为30000，启动成功后你可以直接在浏览器中打开 `http://localhost:30000`

## docker
* docker repo见 [此处](https://hub.docker.com/r/chocotan/bili-recorder)
* 你可以通过 **JAVA_OPT** 这个 **环境变量** 指定启动参数，示例如下 `-Drecord.work-path=/bilirecord -Xmx512m -Xms512m -Xmn256m`

下面是docker命令示例
```shell
docker pull chocotan/bili-recorder:latest
docker run -d -p 30000:30000 -v $HOME/.bili:/bilirecord --env JAVA_OPT="-Drecord.work-path=/bilirecord -Xmx512m -Xms512m -Xmn256m" chocotan/bili-recorder
```
你可以自行修改命令中的端口号和work-path

## ffmpeg
本项目使用ffmpeg来将flv转换为mp4，你需要将ffmpeg添加至PATH中。

## 支持的直播平台
1. bilibili
2. 酷狗繁星直播


## 其他
* B站API的接口和字段定义来自 [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)
* 可用的appkey点击这里 [appkey](https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/other/API_auth.md)
* `src/main/java/struct` 文件夹下的代码为 [javastruct](https://code.google.com/archive/p/javastruct/) 项目的部分源码，本项目未对该部分代码做任何改动，该项目是2007年左右托管在google code上，已经有十多年未维护了，其许可证为LGPL V3。本项目使用其部分代码用于解析b站的弹幕。
* 本项目的`BiliDataUtil` 类修改自 [Bilibili_Danmuji](https://github.com/BanqiJane/Bilibili_Danmuji) ，其许可证为GPL V3。
* 酷狗直播的弹幕解析方式来自于 [real-url](https://github.com/wbt5/real-url)
* 有任何问题可直接新建issue。
