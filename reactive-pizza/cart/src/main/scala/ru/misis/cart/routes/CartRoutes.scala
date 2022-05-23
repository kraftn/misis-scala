package ru.misis.cart.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import spray.json._
import spray.json.DefaultJsonProtocol._
import io.scalaland.chimney.dsl._
import ru.misis.event.EventJsonFormats._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import ru.misis.cart.model.CartCommands
import ru.misis.cart.model.Objects.CartItem
import ru.misis.cart.model.ModelJsonFormats._
import ru.misis.event.OrderStatuses.NotPaidOrder

import java.util.UUID
import scala.concurrent.ExecutionContext

class CartRoutes(cartService: CartCommands)(implicit val system: ActorSystem,
                                            executionContext: ExecutionContext) {

    val routes: Route =
    path("menu") {
        get {
            rejectEmptyResponse {
                onSuccess(cartService.getMenu) { menu =>
                    complete(menu)
                }
            }
        }
    } ~
    pathPrefix("cart" / Segment) { cartId =>
        path("add") {
            (post & entity(as[String])) { json =>
                val cartItem = CartItem(json.parseJson.convertTo[String], 1)

                onSuccess(cartService.updateCart(cartId, cartItem)) { cart =>
                    complete(StatusCodes.Created, cart)
                }
            }
        } ~
        path("change") {
            (put & entity(as[CartItem])) { cartItem =>
                onSuccess(cartService.updateCart(cartId, cartItem)) { cart =>
                    complete(StatusCodes.OK, cart)
                }
            }
        } ~
        path("pay") {
            post {
                onSuccess(cartService.createOrder(cartId, NotPaidOrder)
                    .flatMap(cartService.payForOrder)) { _ =>
                    complete(StatusCodes.OK)
                }
            }
        } ~
        pathEnd {
            delete {
                onSuccess(cartService.deleteCart(cartId)) { _ =>
                    complete(StatusCodes.OK)
                }
            }
        }
    }
}
