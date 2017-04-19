# 安卓下载任务管理
---

> 前言：上年开发了一个壁纸，音乐，应用，视频等资源浏览和下载安卓应用，准备分解功能模块做下笔记。下载页面UI设计参照 **网易云音乐**

下载功能

* 多任务并行下载
* 断点续传（需服务器支持）

项目地址：[https://github.com/4ndroidev/DownloadManager.git](https://github.com/4ndroidev/DownloadManager.git)

<!-- more -->

效果图

![image](https://4ndroidev.github.io/images/android-download-manager/download-screenshot.jpg)

#### 实现原理

下载任务流程图

![image](https://4ndroidev.github.io/images/android-download-manager/download-task-flow.png)

由上图可知，任务执行流程大致如下

1. 创建任务，并做准备，设置监听器等操作
2. 根据任务创建实际下载工作，添加到任务队列，等待或直接执行
3. 用户操作，进行暂停，恢复，或删除

#### 核心类分析

|类|功能|
|---|---|
|DownloadTask|下载任务，保存部分关键信息，非实际下载工作|
|DownloadInfo|下载信息，保存所有信息|
|DownloadJob|实现Runnable接口，实际下载工作，负责网络请求，数据库信息更新|
|DownloadManager|单例，创建下载任务，提供获取正在下载任务，所有下载信息，设置监听器等接口|
|DownloadEngine|负责创建线程池，根据任务创建下载工作，调度工作及通知|
|DownloadProvider|负责下载信息数据库增删查改|

#### 类关联关系

|关联|关系|
|---|---|
| **DownloadTask** - **DownloadInfo** | n - 1 |
| **DownloadTask** - **DownloadJob** | n - 0...1 |
| **DownloadJob** - **DownloadInfo** | 1 - 1 |

#### 下载工作

断点续传的关键点：

- 使用**Range**这个**Header**来指定开始下载位置
- 文件读写则使用**RandomAccessFile**，可在指定偏移量读写文件
- 注意**RandomAccessFile**打开模式不要加入`s`，同步模式会拖慢下载速度


#### 使用说明

```java
//创建任务
DownloadTask task = DownloadManager.get(context)
	.download(id, url, name).listener(listener).create();

//启动任务
task.start();

//暂停任务
task.pause();

//恢复任务
task.resume();

//删除任务
task.remove();
```