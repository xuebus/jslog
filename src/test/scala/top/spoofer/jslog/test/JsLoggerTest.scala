package top.spoofer.jslog.test

import top.spoofer.jslog.JsLogger
import top.spoofer.jslog.unit.JsLogLevel.{Info, Error}
import top.spoofer.jslog.unit.LogCreator

object JsLoggerTest {
  def main(args: Array[String]) {
    val i = Info(LogCreator(this.getClass, this.hashCode()), "test into", flush = false)
    println(i.logMsgFormat())

    val e = Error(LogCreator(this.getClass, this.hashCode()), "test into", new Exception("exla"), flush = false)
    println(e.logMsgFormat())
    //    println(i.logLevel)
    //    println(i.logMsgFormat())
    //    println(i.logMsg)
    //    println(i.creator.logClass)
    //    println(i.creator.addr)
    println("=====")
  }
}
