package top.spoofer.jslog.test

import top.spoofer.jslog.{JsLogger, JsLoggerConfiguration}

/**
  * Created by lele on 16-3-18.
  */
object JsLoggerSystemTest {
  def main(args: Array[String]) {
    val cfg = new JsLoggerConfiguration()
    cfg.appName = "test"
    cfg.cacheThreshold = 16
    cfg.flushInterval = 3600
    cfg.FSYNC_WAL = false

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
}
