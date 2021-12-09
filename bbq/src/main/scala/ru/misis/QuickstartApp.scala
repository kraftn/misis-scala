package ru.misis

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import ru.misis.registry.{ItemRegistry, MenuRegistry, UserRegistry}
import ru.misis.routes.{ItemRoutes, MenuRoutes, UserRoutes}
import akka.http.scaladsl.server.Directives._
import ru.misis.services.{InitDB, ItemServiceImpl}
import slick.jdbc.PostgresProfile.api._

import scala.util.Failure
import scala.util.Success

//#main-class
object QuickstartApp {
    //#start-http-server
    private def startHttpServer(routes: Route)(implicit system: ActorSystem[_]): Unit = {
        // Akka HTTP still needs a classic ActorSystem to start
        import system.executionContext

        val futureBinding = Http().newServerAt("localhost", 8080).bind(routes)
        futureBinding.onComplete {
            case Success(binding) =>
                val address = binding.localAddress
                system.log.info("Server online at http://{}:{}/", address.getHostString, address.getPort)
            case Failure(ex) =>
                system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
                system.terminate()
        }
    }

    lazy val db = Database.forConfig("database.postgres")

    //#start-http-server
    def main(args: Array[String]): Unit = {
        //#server-bootstrapping
        val rootBehavior = Behaviors.setup[Nothing] { context =>

            implicit val contextImpl = context.system
            implicit val executionContext = contextImpl.executionContext

            trait init {
                def db = QuickstartApp.db
                implicit val executionContext = contextImpl.executionContext
            }

            new InitDB(db).prepareRepository()

            val userRegistryActor = context.spawn(UserRegistry(), "UserRegistryActor")
            context.watch(userRegistryActor)
            val itemRegistry = new ItemRegistry() with ItemServiceImpl with init
            val itemRegistryActor = context.spawn(itemRegistry(), "ItemRegistryActor")
            context.watch(itemRegistryActor)
            val menuRegistryActor = context.spawn(new MenuRegistry(itemRegistryActor).apply(), "MenuRegistryActor")
            context.watch(menuRegistryActor)

            val userRoutes = new UserRoutes(userRegistryActor)(context.system)
            val menuRoutes = new MenuRoutes(menuRegistryActor)
            val itemRoutes = new ItemRoutes(itemRegistryActor)
            startHttpServer(userRoutes.routes ~ menuRoutes.routes ~ itemRoutes.routes)(context.system)

            Behaviors.empty
        }
        val system = ActorSystem[Nothing](rootBehavior, "HelloAkkaHttpServer")
        //#server-bootstrapping
    }
}

//#main-class
