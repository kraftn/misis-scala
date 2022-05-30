package ru.misis.menu

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.sksamuel.elastic4s.http.JavaClient
import com.sksamuel.elastic4s.{ElasticClient, ElasticProperties}
import ru.misis.menu.routes.MenuRoutes
import ru.misis.menu.service.{MenuCommandImpl, MenuEventProcessing}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Failure
import scala.util.Success

object MenuApp {
  private def startHttpServer(routes: Route)(implicit system: ActorSystem): Unit = {
    import system.dispatcher

    val futureBinding = Http().newServerAt("0.0.0.0", 8080).bind(routes)
    futureBinding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
      case Failure(ex) =>
        system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
        system.terminate()
    }
  }

  val props = ElasticProperties(s"http://${sys.env("ELASTIC")}")
  val elastic = ElasticClient(JavaClient(props))

  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("HelloAkkaHttpServer")
    val menuCommands = new MenuCommandImpl(elastic)
    val routes = new MenuRoutes(menuCommands)
    val menuEventProcessing = new MenuEventProcessing(menuCommands)
    startHttpServer(routes.routes)

  }
}
