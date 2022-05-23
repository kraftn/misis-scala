package ru.misis.order.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._
import io.scalaland.chimney.dsl._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import ru.misis.order.model.OrderCommands
import ru.misis.event.EventJsonFormats._
import spray.json._

class OrderRoutes(orderService: OrderCommands)(implicit val system: ActorSystem){

    val routes: Route =
    path("orders") {
        get {
            onSuccess(orderService.listOrders()) { orders =>
                complete(orders)
            }
        }
    } ~
    path("take" / Segment) { cartId =>
        post {
            onSuccess(orderService.takeOrder(cartId)) { _ =>
                complete(StatusCodes.OK)
            }
        }
    } ~
    path("form" / Segment) { cartId =>
        post {
            onSuccess(orderService.formOrder(cartId)) { _ =>
                complete(StatusCodes.OK)
            }
        }
    } ~
    path("complete" / Segment) { cartId =>
        post {
            onSuccess(orderService.completeOrder(cartId)) { _ =>
                complete(StatusCodes.OK)
            }
        }
    } ~
    path("status" / Segment) { cartId =>
        get {
            onSuccess(orderService.getStatus(cartId)) { status =>
                complete(status)
            }
        }
    }
}
