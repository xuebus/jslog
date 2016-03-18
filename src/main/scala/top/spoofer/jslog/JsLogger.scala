/**
  * Copyright (c) 2016, BoDao, Inc. All Rights Reserved.
  *
  * Author@ dgl
  *
  * 本程序是jslog对外的API接口类
  * 提供初始化jslog系统
  * 异步写入各种级别的log
  */

package top.spoofer.jslog

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Props, ActorRef, ActorSystem}
import top.spoofer.jslog.unit.JsLogLevel._
import top.spoofer.jslog.unit.{JsLogFileIO, LogCreator}

object JsLogger {
  private var initialized = false
  private var jsLoggerConfiguration: Option[JsLoggerConfiguration] = None
  private var jsLoggerSystem: Option[ActorSystem] = None
  private var jsLoggerRouter: Option[ActorRef] = None

  private def startJslLoggerSystem() = {
    val system = ActorSystem("JsLoggerSystem")
    jsLoggerSystem = Some(system)
    jsLoggerRouter = Some(system.actorOf(Props[JsLoggerRouter], "JsLoggerRouter"))
  }

  def apply(jsCfg: JsLoggerConfiguration): Unit = synchronized {
    if (!initialized) {
      this.jsLoggerConfiguration = Some(jsCfg)
      JsLogFileIO.createDir(jsCfg.savePath) //此处不成功会抛出异常, jslog系统不会启动
      this.startJslLoggerSystem()
      initialized = true
    } else throw new Exception("JsLogger was Initialized")
  }

  def apply(): Unit = {
    this.apply(new JsLoggerConfiguration())
  }

  def getConfig: Option[JsLoggerConfiguration] = this.jsLoggerConfiguration

  //对内的api,简化操作
  private def newCreator(obj: AnyRef): LogCreator = LogCreator(obj.getClass, obj.hashCode())

  def infoLog(obj: AnyRef, logMsg: String = "", flush: Boolean = false) = {
    jsLoggerRouter match {
      case None => throw new Exception("JsLoggerSystem never init")
      case Some(actor) => actor ! Info(this.newCreator(obj), logMsg, flush)
    }
  }

  def debugLog(obj: AnyRef, logMsg: String = "", flush: Boolean = false) = {
    jsLoggerRouter match {
      case None => throw new Exception("JsLoggerSystem never init")
      case Some(actor) => actor ! Debug(this.newCreator(obj), logMsg, flush)
    }
  }

  def warningLog(obj: AnyRef, logMsg: String = "", flush: Boolean = false) = {
    jsLoggerRouter match {
      case None => throw new Exception("JsLoggerSystem never init")
      case Some(actor) => actor ! Warning(this.newCreator(obj), logMsg, flush)
    }
  }

  def errorLog(obj: AnyRef, logMsg: String = "",
               exceStackTrace: Throwable = NoExceStackTrace, flush: Boolean = false) = {
    jsLoggerRouter match {
      case None => throw new Exception("JsLoggerSystem never init")
      case Some(actor) => actor ! Error(this.newCreator(obj), logMsg, exceStackTrace, flush)
    }
  }

  /**
    * 将cache里的log写到文件里
    */
  private def forceFlushLogs() = {
    jsLoggerRouter match {
      case None =>    //不需要理会， 因为要关闭系统了
      case Some(router) => router ! Stop
    }
  }

  def closeJsLoggerSystem() = synchronized {
    if (this.initialized) {
      jsLoggerSystem match {
        case None => throw new Exception("JsLoggerSystem never init")
        case Some(system) =>
          this.infoLog(this, "start to stop JsLoggerSystem, sleep 1s to wait JsLoggerSystem ...", flush = true)
          this.forceFlushLogs()
          println("sleep 1s to wait JsLoggerSystem ...")
          Thread.sleep(1000)  //等待刷写文件
          system.shutdown()
          println("JsLoggerSystem stoped ...")
      }
    } else throw new Exception("JsLoggerSystem never init")
  }
}