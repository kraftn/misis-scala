package ru.misis.waiter_bot

import akka.actor.ActorSystem
import ru.misis.waiter_bot.service.{WaiterCommandImpl, WaiterEventProcessing}

import scala.concurrent.ExecutionContext.Implicits.global

object WaiterApp {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")
    val waiterCommands = new WaiterCommandImpl()
    val waiterEventProcessing = new WaiterEventProcessing(waiterCommands)
  }
}
