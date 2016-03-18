package top.spoofer.jslog.test

import akka.actor.{ActorSystem, Props}

import top.spoofer.jslog.{InfoLoggerActor, JsLog}
import top.spoofer.jslog.unit.JsLogLevel.Info
import top.spoofer.jslog.unit.LogCreator

object JsLoggerActorTest {
  def main(args: Array[String]) {
    implicit val system = ActorSystem()
    val infoLogger = system.actorOf(Props[InfoLoggerActor], "infoLogger")

    Thread.sleep(2000)

    for (i <- 1 to 11) {
      Thread.sleep(1000)
      infoLogger ! JsLog(Info(LogCreator(this.getClass, this.hashCode()), s"$i"))
    }

    Thread.sleep(1000)
    infoLogger ! JsLog(Info(LogCreator(this.getClass, this.hashCode()), "12", flush = true))

    for (i <- 13 to 15) {
      Thread.sleep(1000)
      infoLogger ! JsLog(Info(LogCreator(this.getClass, this.hashCode()), s"$i"))
    }

    Thread.sleep(1000)
    infoLogger ! JsLog(Info(LogCreator(this.getClass, this.hashCode()), "16", flush = true))
  }
}
