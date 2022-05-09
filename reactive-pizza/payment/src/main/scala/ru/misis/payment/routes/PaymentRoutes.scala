package ru.misis.payment.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json.DefaultJsonProtocol._
import io.scalaland.chimney.dsl._
import ru.misis.event.EventJsonFormats._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import ru.misis.event.Cart.Order
import ru.misis.payment.model.PaymentCommands
import spray.json._

class PaymentRoutes(paymentService: PaymentCommands)(implicit val system: ActorSystem) {
    val routes: Route =
    path("pay") {
        (post & entity(as[Order])) { order =>
            onSuccess(paymentService.makePayment(order)) { cheque =>
                complete(cheque)
            }
        }
    }
}
