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
import ru.misis.event.OrderStatuses.TakenOrder
import spray.json._

import scala.concurrent.ExecutionContext

class OrderRoutes(orderService: OrderCommands)(implicit val system: ActorSystem,
                                               executionContext: ExecutionContext) {

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
            onSuccess(orderService.takeOrder(cartId).flatMap { _ =>
                orderService.updateOrderStatus(cartId, TakenOrder)
            }) { _ =>
                complete(StatusCodes.OK)
            }
        }
    } ~
    path("status" / Segment) { cartId =>
        get {
            onSuccess(orderService.getOrderStatus(cartId)) { status =>
                complete(status)
            }
        }
    }
}
