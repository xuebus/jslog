/**
  * Copyright (c) 2016, BoDao, Inc. All Rights Reserved.
  *
  * Author@ dgl
  *
  * 本程序是jslog实现日志异步写入文件的关键
  * 根据日志的级别，发送给不同的actor进行日志缓存和写入操作
  *
  * 当其监管的子actor异常退出后，进行重启退出的子actor
  */

package top.spoofer.jslog

import akka.actor.SupervisorStrategy.{Stop, Restart}
import akka.actor.{OneForOneStrategy, Props, ActorRef, Actor}
import top.spoofer.jslog.unit.LogLevelEnum._
import top.spoofer.jslog.unit.{LogLevelEnum, JsLogEvent}
import top.spoofer.jslog.unit.JsLogLevel.NoExceStackTrace

class JsLoggerRouter extends Actor {
  private var logWorkers: Option[Map[LogLevelEnum, ActorRef]] = _

  private def initInfoWorker(): (LogLevelEnum, ActorRef) = {
    val infoWorker = context.actorOf(Props[InfoLoggerActor], "InfoLoggerActor")
    context.watch(infoWorker)
    (LogLevelEnum.Info, infoWorker)
  }

  private def initDebugWorker(): (LogLevelEnum, ActorRef) = {
    val debugWorker = context.actorOf(Props[DebugLoggerActor], "DebugLoggerActor")
    context.watch(debugWorker)
    (LogLevelEnum.debug, debugWorker)
  }

  private def initWarningWorker(): (LogLevelEnum, ActorRef) = {
    val warningWorker = context.actorOf(Props[WarningLoggerActor], "WarningLoggerActor")
    context.watch(warningWorker)
    (LogLevelEnum.Warn, warningWorker)
  }

  private def initErrorWorker(): (LogLevelEnum, ActorRef) = {
    val errorWorker = context.actorOf(Props[ErrorLoggerActor], "ErrorLoggerActor")
    context.watch(errorWorker)
    (LogLevelEnum.Err, errorWorker)
  }


  private def initLogWorkers(): Option[Map[LogLevelEnum, ActorRef]] = {
    val infoWorker = this.initInfoWorker()
    val debugWorker = this.initDebugWorker()
    val warningWorker = this.initWarningWorker()
    val errorWorker = this.initErrorWorker()

    Some(Map[LogLevelEnum, ActorRef](infoWorker, debugWorker, warningWorker, errorWorker))
  }

  //异常重启
  override val supervisorStrategy = OneForOneStrategy() {
    case ex: Exception => Restart
  }

  override def preStart() = {
    this.logWorkers = initLogWorkers()
  }

  private def stopRouter()= {
    logWorkers match {
      case None => println("logWorkers never inited")
      case Some(works) =>
        works.keys foreach { workerLevel =>
          val workActor = works(workerLevel)
          workActor ! LogsFlush //强制刷写
        }
    }
  }

  private def routerLog(logEvent: JsLogEvent) = {
    logWorkers match {
      case None => println("logWorkers never inited")
      case Some(works) =>
        works.get(logEvent.logLevel) match {
          case None => println(s"${logEvent.logLevel} level work not fount")
          case Some(workActor) => workActor ! JsLog(logEvent)
        }
    }
  }

  def receive = {
    case logEvent: JsLogEvent => this.routerLog(logEvent)
    case Stop => this.stopRouter()  //停止系统
    case _ => JsLogger.errorLog(this, "receive a unknow msg", NoExceStackTrace, flush = false)
  }
}
