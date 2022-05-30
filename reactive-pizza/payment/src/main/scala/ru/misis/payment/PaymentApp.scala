package ru.misis.payment

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import ru.misis.payment.routes.PaymentRoutes
import ru.misis.payment.service.PaymentCommandImpl

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

object PaymentApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem): Unit = {
    import system.dispatcher

    val futureBinding = Http().newServerAt("0.0.0.0", 8082).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")
    val menuCommands = new PaymentCommandImpl
    val routes = new PaymentRoutes(menuCommands)
    startHttpServer(routes.routes)
  }
}
