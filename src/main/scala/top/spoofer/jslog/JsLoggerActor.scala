/**
 * Copyright (c) 2016, BoDao, Inc. All Rights Reserved.
 *
 * Author@ cwx
 *
 * 本程序是一个log actor， 当接收到log消息Info，Warn或Err时，会
 * 把消息缓存入队列中，当收到定时的Flush消息时或带有flush性质的
 * 消息时，会把缓存队列的消息保存到文件中
 */

package top.spoofer.jslog

import scala.collection.mutable.{ArrayBuffer, Map => MuMap}
import scala.concurrent.duration._
import scala.language.implicitConversions
import akka.actor.{Actor, FSM}
import top.spoofer.jslog.unit.{JsLogFileIO, JsLogEvent}

sealed trait LogsState
final case class LogsInit()
case object LogsStart extends LogsState
case object LogsRunning extends LogsState
case object LogsFill extends LogsState

sealed trait LogsData
case object LogsUninitialized extends LogsData
case class JsLog(jsLog: JsLogEvent) extends LogsData
case class JsLogsCache(jsLogsCache: ArrayBuffer[JsLogEvent]) extends LogsData

case object LogsFlush

sealed trait JsLoggerTrait extends Actor with FSM[LogsState, LogsData] {
  private val jsLogs = ArrayBuffer[JsLogEvent]()
  private val config = JsLogger.getConfig.get
  private val flushInterval = config.flushInterval  //日志信息刷写的时间间隔, 单位秒
  private val cacheThreshold = config.cacheThreshold  //日志缓存队列的大小, 最小4个
  private val savePath = config.savePath
  private val appName = config.appName
  private val FSYNC = config.FSYNC_WAL
  protected val logType: String

  private implicit def cacheToString(buffer: TraversableOnce[JsLogEvent]): TraversableOnce[String] = {
    val strList = buffer.map(jsLogEvent => jsLogEvent.logMsgFormat())
    strList
  }

  private def flushCache(buffer: TraversableOnce[JsLogEvent]): Boolean = {
    val muMap = MuMap[String, ArrayBuffer[JsLogEvent]]()

    /**
      * 解决这次flush下有多日的日志， 为什么会出现这样的情况呢？
      * 在极端的情况下，本次缓存的logevent是两日甚至是多日（配置的刷写时间过长， 缓存没有满过）的
      * 所以需要按日期分类写入不同的文件，其中文件名字是用日期区分的。
      */
    buffer.foreach (jsLogEvent => {
      if (muMap.contains(jsLogEvent.date)) muMap(jsLogEvent.date) += jsLogEvent
      else {
        val cache = ArrayBuffer[JsLogEvent]()
        cache += jsLogEvent
        muMap += (jsLogEvent.date -> cache)
      }
    })

    this.writeToFile(muMap.toMap)
    true
  }

  private def writeToFile(map: Map[String, TraversableOnce[JsLogEvent]]): Boolean = {
    map.keys foreach (date => {
      if (map(date).nonEmpty) {
        JsLogFileIO.save(this.getLogFileName(date, logType, this.appName), map(date), append = true, this.FSYNC)
      }
    })
    true
  }

  private def getLogFileName(date: String = "", logType: String = "", appName: String = "") = {
    s"${savePath.stripSuffix("/")}/$appName-$date-$logType.log"
  }

  setTimer("JsLogsTimer", LogsFlush, this.flushInterval.seconds, repeat = true)

  startWith(LogsStart, LogsUninitialized)

  when(LogsStart) {
    case Event(LogsInit, LogsUninitialized) => stay using JsLogsCache(jsLogs)
    case Event(JsLog(jsLog), jsLogsCache @ JsLogsCache(buffer)) =>
      buffer += jsLog
      if (jsLog.flush) {
        this.flushCache(buffer)
        buffer.clear()
        stay using jsLogsCache.copy(jsLogsCache = buffer)
      } else {
        goto(LogsRunning) using jsLogsCache.copy(jsLogsCache = buffer)
      }

    case Event(LogsFlush, jsLogsCache @ JsLogsCache(buffer)) =>
      stay using jsLogsCache.copy(jsLogsCache = buffer)
  }

  when(LogsRunning) {
    case Event(JsLog(jsLog), jsLogsCache @ JsLogsCache(buffer)) =>
      buffer += jsLog
      if (jsLog.flush) {
        this.flushCache(buffer)
        buffer.clear()
        goto(LogsStart) using jsLogsCache.copy(jsLogsCache = buffer)
      } else {
        buffer.length match {
          case this.cacheThreshold => goto(LogsFill) using jsLogsCache.copy(jsLogsCache = buffer)
          case _ => stay using jsLogsCache.copy(jsLogsCache = buffer)
        }
      }

    case Event(LogsFlush, jsLogsCache @ JsLogsCache(buffer)) =>
      this.flushCache(buffer)
      buffer.clear()
      goto(LogsStart) using jsLogsCache.copy(jsLogsCache = buffer)
  }

  when(LogsFill) {
    case Event(JsLog(jsLog), jsLogsCache @ JsLogsCache(buffer)) =>
      if (jsLog.flush) {
        buffer += jsLog
        this.flushCache(buffer)
        buffer.clear()
        goto(LogsStart) using jsLogsCache.copy(jsLogsCache = buffer)
      } else {
        this.flushCache(buffer)
        buffer.clear()
        buffer += jsLog
        goto(LogsRunning) using jsLogsCache.copy(jsLogsCache = buffer)
      }

    case Event(LogsFlush, jsLogsCache @ JsLogsCache(buffer)) =>
      this.flushCache(buffer)
      buffer.clear()
      goto(LogsStart) using jsLogsCache.copy(jsLogsCache = buffer)
  }

  initialize()
}

class InfoLoggerActor extends JsLoggerTrait {
  override val logType = "Info"

  override def preStart() = {
    self ! LogsInit
  }
}

class ErrorLoggerActor extends JsLoggerTrait {
  override val logType = "Error"

  override def preStart() = {
    self ! LogsInit
  }
}

class WarningLoggerActor extends JsLoggerTrait {
  override val logType = "Warning"

  override def preStart() = {
    self ! LogsInit
  }
}

class DebugLoggerActor extends JsLoggerTrait {
  override val logType = "Debug"

  override def preStart() = {
    self ! LogsInit
  }
}