package ru.misis.cart

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.cart.routes.CartRoutes
import ru.misis.cart.service.{CartCommandImpl, CartEventProcessing}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

object CartApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem): Unit = {
    import system.dispatcher

    val futureBinding = Http().newServerAt("localhost", 8081).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  val props = ElasticProperties("http://localhost:9200")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")
    val cartCommands = new CartCommandImpl(elastic)
    val routes = new CartRoutes(cartCommands)
    val cartEventProcessing = new CartEventProcessing(cartCommands)
    startHttpServer(routes.routes)
  }
}
