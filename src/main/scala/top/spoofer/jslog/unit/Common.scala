/**
  * Copyright (c) 2016, BoDao, Inc. All Rights Reserved.
  *
  * Author@ dgl
  *
  * 本程序定义jslog的日志等级， 分别为：
  * info、debug、warning、error
  * 每个等级都有自己的格式化函数
  */

package top.spoofer.jslog.unit

import java.io.File

import scala.language.existentials
import akka.actor.NoSerializationVerificationNeeded
import top.spoofer.jslog.unit.LogLevelEnum.LogLevelEnum

/**
  * 日志级别
  */
object LogLevelEnum extends Enumeration {
  type LogLevelEnum = Value
  val Info = Value
  val Warn = Value
  val Err = Value
  val debug = Value
}

/**
  * 日子创造者, 类名字和hashcode
  * @param logClass 类名字
  * @param addr hashcode
  */
case class LogCreator(logClass: Class[_], addr: Int = 0)

/**
  * 定义日志事件模板
  */
sealed trait JsLogEvent extends NoSerializationVerificationNeeded {
  import java.text.SimpleDateFormat
  import scala.language.implicitConversions

  implicit def logCreatorToString(creator: LogCreator): String = {
    s"${creator.getClass.getName} @ ${Integer.toHexString(creator.hashCode())}"
  }

  implicit def timestamp2Date(timestamp: Long): String = {
    this.secondFormat.format(timestamp)
  }

  private val secondFormat = new SimpleDateFormat("yyyy-MM-dd")
  protected def creator: LogCreator
  protected def logMsg: String
  val date: String = System.currentTimeMillis

  def logLevel: LogLevelEnum
  def flush: Boolean
  def logMsgFormat(): String = {
    val logFormat = "{\n\tlevel:  %s\n\tcreator:  %s\n\tdate: %s\n\tmsg:  %s\n}\n"
    logFormat.format(logLevel, logCreatorToString(creator), this.date, logMsg)
  }
}

object JsLogLevel {
  import scala.util.control.NoStackTrace
  object NoExceStackTrace extends NoStackTrace

  case class Info(creator: LogCreator, logMsg: String = "", flush: Boolean = false) extends JsLogEvent {
    override def logLevel = LogLevelEnum.Info
  }

  case class Debug(creator: LogCreator, logMsg: String = "", flush: Boolean = false) extends JsLogEvent {
    override def logLevel = LogLevelEnum.debug
  }

  case class Warning(creator: LogCreator, logMsg: String = "", flush: Boolean = false) extends JsLogEvent {
    override def logLevel = LogLevelEnum.Warn
  }

  case class Error(creator: LogCreator, logMsg: String = "",
                   exceStackTrace: Throwable, flush: Boolean = false) extends JsLogEvent {
    override def logLevel = LogLevelEnum.Err

    private def logMsgFormat4Exection(): String = {
      val logFormat = "{\n\tlevel:  %s\n\tcreator:  %s\n\tdate: %s\n\tmsg:  %s\n\texception:  %s\n}\n"
      logFormat.format(logLevel, logCreatorToString(creator),
        this.date, logMsg, exceStackTrace.getStackTraceString.replace("\n", "\n\t\t\t\t"))
    }

    override def logMsgFormat(): String = {
      exceStackTrace match {
        case NoExceStackTrace => super.logMsgFormat()
        case null => super.logMsgFormat()
        case ex => this.logMsgFormat4Exection()
      }
    }
  }
}

object JsLogFileIO {
  import java.io.{BufferedOutputStream, FileOutputStream}

  /**
    * 将数据存储到文件
    * 当apend为true时，追加内容到文件中
    * 如果需要换行，需要在每个content元素的尾部加入'\n'
    * @param fileName 文件名字
    * @param contents 内容
    * @param append 是否追加
    * @param fsync  是否同步刷写磁盘
    * @return
    */
  def save(fileName: String, contents: TraversableOnce[String],
           append: Boolean = false, fsync: Boolean = false): Boolean = {
    var status = false
    var fos: FileOutputStream = null
    var bos: BufferedOutputStream = null
    try {
      fos = new FileOutputStream(fileName, append)
      bos = new BufferedOutputStream(fos)
      contents foreach { content =>
        bos.write(content.getBytes)
      }
      if (fsync) bos.flush()
      status = true
    } catch {
      case ex: Exception =>
        println(ex.getStackTraceString)
        status = false
    } finally {
      if (bos != null) bos.close()
      if (fos != null) fos.close()
    }
    status
  }

  /**
    * 创建一个目录，如果这个目录的父目录不存在，会同时创建这个父目录
    * @param dir 目录名字
    * @return
    */
  def createDir(dir: String): Boolean = {
    try {
      val file = new File(dir)
      file.mkdirs()
      true
    } catch {
      case ex: Exception => throw new Exception(s"create $dir fail, ${ex.getStackTraceString}")
    }
  }
}