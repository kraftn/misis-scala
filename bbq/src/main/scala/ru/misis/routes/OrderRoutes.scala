package ru.misis.routes

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import ru.misis.registry.OrderRegistry
import ru.misis.registry.OrderRegistry.{ChangeStatus, CreateOrder, DeleteOrder, GetOrder, GetOrders, GetOrdersByStatus, OrderDto, UpdateOrder}

class OrderRoutes(orderRegistry: ActorRef[OrderRegistry.Command])(implicit val system: ActorSystem[_]){
    import ru.misis.JsonFormats._
    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._

    private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

    val routes: Route =
        path("orders") {
            get {
                complete(orderRegistry.ask(GetOrders))
            }
        } ~
        path("orders") {
            (post & entity(as[OrderDto])) { order =>
                onSuccess(orderRegistry.ask(CreateOrder(order, _))) { performed =>
                    complete((StatusCodes.Created, performed))
                }
            }
        } ~
        path("orders" / Segment) { status =>
            get {
                complete(orderRegistry.ask(GetOrdersByStatus(status, _)))
            }
        } ~
        path("order" / Segment) { id =>
            get {
                rejectEmptyResponse {
                    onSuccess(orderRegistry.ask(GetOrder(id.toInt, _))) { response =>
                        complete(response.maybe)
                    }
                }
            }
        } ~
        path("order" / Segment) { id =>
            (put & entity(as[OrderDto])) { order =>
                onSuccess(orderRegistry.ask(UpdateOrder(id.toInt, order, _))) { performed =>
                    complete((StatusCodes.OK, performed))
                }
            }
        } ~
        path("order" / Segment) { id =>
            put {
                parameters(Symbol("status").as[String]) { status =>
                    onSuccess(orderRegistry.ask(ChangeStatus(id.toInt, status, _))) { performed =>
                        complete((StatusCodes.OK, performed))
                    }
                }
            }
        } ~
        path("order" / Segment) { id =>
            delete {
                onSuccess(orderRegistry.ask(DeleteOrder(id.toInt, _))) { performed =>
                    complete((StatusCodes.OK, performed))
                }
            }
        }
}
