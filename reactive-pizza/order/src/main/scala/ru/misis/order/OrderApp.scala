package ru.misis.order

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.order.routes.OrderRoutes
import ru.misis.order.service.{OrderCommandImpl, OrderEventProcessing}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

object OrderApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem): Unit = {
    import system.dispatcher

    val futureBinding = Http().newServerAt("localhost", 8083).bind(routes)
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
    val orderCommands = new OrderCommandImpl(elastic)
    val routes = new OrderRoutes(orderCommands)
    val orderEventProcessing = new OrderEventProcessing(orderCommands)
    startHttpServer(routes.routes)
  }
}
