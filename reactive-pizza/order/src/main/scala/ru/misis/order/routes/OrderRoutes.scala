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
    path("take") {
        (put & entity(as[String])) { json =>
            onSuccess(orderService.takeOrder(json.parseJson.convertTo[String])) { _ =>
                complete(StatusCodes.OK)
            }
        }
    }
}
