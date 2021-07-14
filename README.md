# bilibili直播录播工具

[![Maven](https://github.com/chocotan/bili-recorder/actions/workflows/maven.yml/badge.svg)](https://github.com/chocotan/bili-recorder/actions/workflows/maven.yml)

这是一个大概开发好已经能用的录播工具

## 打包
* 你需要Java 11和maven才能正常打包
* 运行命令 `mvn clean package -Dmaven.test.skip`
* 打包完成后会在`target`目录生成可执行jar

## 配置
* record.work-path: 工作目录，不管是h2数据库文件、日志，还是录播位置，默认值为`${user.home}/.bili`
* record.check-interval: 检查直播间状态的间隔，单位是秒，默认值为40
* server.port: 启动后的端口，默认值为30000

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


## 感谢
* B站API的接口和字段定义来自 [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect)
