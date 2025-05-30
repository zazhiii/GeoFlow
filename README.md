# GeoFlow
GeoFlow 是一个基于 Java 的遥感图像处理系统，旨在提供高效、可靠的工具来处理和分析遥感数据。
## 功能特性
- 遥感图像处理：提供多种遥感图像的处理和分析功能，支持常见的遥感数据格式。

- 可扩展性：系统采用模块化设计，方便开发者根据需求添加新的功能模块。

- 高性能：利用 Java 的多线程和高效的算法，确保处理大规模遥感数据时的性能。

## 目录结构

## 技术选型

## 部署
### 环境要求
| 组件      | 版本推荐                              |
| ------- | --------------------------------- |
| JDK     | 17+                               |
| MySQL   | 8.0+                              |
| MinIO   | RELEASE.2023+                     |
| Maven   | 3.8+                              |
| Git     | 可选（用于拉取仓库）                        |
| 操作系统    | Windows / Linux（推荐 Ubuntu 20.04+） |

### 数据库配置
1. 安装并启动MySQL
2. 运行doc/database.sql（创建数据库和表结构）
3. 修改项目的数据库链接配置
```yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/geo_flow?useSSL=false&serverTimezone=UTC # 修改为你的数据库ip + 端口
    username: root  # 修改
    password: 123456  # 修改
```
### 对象存储（MinIO）配置
1. 下载并启动MinIO，并创建AccessKey和AccessSecret
2. 修改项目的MinIO配置
```yml
minio:
  endpoint: http://localhost:9000 # 修改为你的MinIO运行的ip + 端口
  bucketName: geoflow # 这个可以不修改
  accessKey: J9oSiTvI3Pf2yRmd0CrS # 修改
  secretKey: QjhGtPnUaLY5fuzg3HSrSEq4wx5HzqqjYQwddVA8 # 修改
```
### 后端运行
1. 命令行构建
```bash
mvn clean install
```
2. 启动SpringBoot项目
```bash
java -jar target/GeoFlow-0.0.1-SNAPSHOT.jar
```
## 贡献指南
欢迎对 GeoFlow 项目提出改进建议或贡献代码。请遵循以下步骤：

Fork 仓库：点击页面右上角的 "Fork" 按钮，将仓库复制到您的 GitHub 账户。

clone 您的 Fork：在本地 clone 隆您 Fork 的仓库。

创建新分支：为您的改动创建一个新的分支。

提交更改：在新分支上进行开发，并提交更改。

发起 Pull Request：将您的更改提交到主仓库的 master 分支。
