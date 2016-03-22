#top.spoofer.jslog

### 简介
top.spoofer.jslog（just simple logger） 一个使用scala和akka开发的简单的、异步日志系统。日志存储在本地的文件系统指定的目录。

jslog的日志等级分为info、debug、warning、error。这个日志系统功能简单所以依赖也很少，这是设计这个日志系统的初衷！

### 安装jslog

由于本项目还没有提交到mvn，所以只能先下载到本地编译打包，然后将打包的jar放到您项目的lib里。

```
$sbt compile
sbt> package
```

### API

#### 配置日志

```
val cfg = new JsLoggerConfiguration()
cfg.appName = "test"  //设置程序名字
cfg.cacheThreshold = 16 //设置缓存的大小， 当系统日志个数大于等于这个数字时会批量写入文件
cfg.flushInterval = 3600  //批量写入日志的时间间隔，单位秒
cfg.FSYNC_WAL = false   //批量写入时是否同步写，即等待数据写入到磁盘再返回， 这个设置true也不会阻塞调用者。为保证数据全部写入， 建议设置true。
cfg.savePath = "./testlog"  //日志存储的路径
```

#### 初始化

```
def apply(jsCfg: JsLoggerConfiguration): Unit = {}  //使用特定的配置初始化系统
def apply(): Unit = {}  //使用默认的配置初始化系统
```

在系统没有初始化的情况下，jslog系统不可用！所以在使用jslog系统前必须先使用上述两个函数初始化系统。


#### 日志记录API

每个日志等级的API如下：

```
def infoLog(obj: AnyRef, logMsg: String = "", flush: Boolean = false) = {}
def debugLog(obj: AnyRef, logMsg: String = "", flush: Boolean = false) = {}
def warningLog(obj: AnyRef, logMsg: String = "", flush: Boolean = false) = {}
def errorLog(obj: AnyRef, logMsg: String = "", exceStackTrace: Throwable = NoExceStackTrace, flush: Boolean = false) = {}
```
obj： 产生日志的对象

logMsg： 日志信息

flush： 立刻刷写的标志

exceStackTrace： 异常栈信息

当flush为true的时候， 这时不管系统缓存是否已满、不管是否到达批量写入文件的时间，系统的缓存的日志都会立刻写入文件。


#### 关闭系统

使用完jslog后需要关闭系统， 一般是在程序关闭的最后进行关闭jslog。

```
def closeJsLoggerSystem() = {}
```

### 例子

```
import top.spoofer.jslog.{JsLoggerConfiguration, JsLogger}

object TestLoggerSystem extends App {
  val cfg = new JsLoggerConfiguration()
  cfg.appName = "test"  //设置程序名字
  cfg.cacheThreshold = 16 //设置缓存的大小， 当系统日志个数大于等于这个数字时会批量写入文件
  cfg.flushInterval = 3600  //批量写入日志的时间间隔，单位秒
  cfg.FSYNC_WAL = false   //批量写入时是否同步写，即等待数据写入到磁盘再返回， 这个设置true也不会阻塞调用者。为保证数据全部写入， 建议设置true。

  JsLogger.apply(cfg)

  for (i <- 1 to 1000) {
    Thread.sleep(1800000)
    JsLogger.infoLog(this, s"$i")
    JsLogger.debugLog(this, s"$i")
    JsLogger.warningLog(this, s"$i")
    JsLogger.errorLog(this, s"$i")
  }

  JsLogger.closeJsLoggerSystem()

  println("====================")
}
```

### 服务对象

如果您开发的是一些中小的系统，不想依赖过大，可以使用本程序。如果是大型系统请使用其他专业的log系统！

### 注意

如果在缓存写入到磁盘之前， 系统挂掉，所有的日志缓存都会丢失！因为缓存都在内存当中。

本系统只负责写日志，不负责删除日志， 如果您使用本系统的话请您及时清理过期的日志以免占用过多的磁盘空间。

因个人精力和水平有限，如果出现bug的话请指出和谅解。

### 致谢

感谢您使用和留意本系统！

感谢@cwx_
