package ru.misis.cart.model

import ru.misis.event.Menu._
import Objects._
import akka.http.scaladsl.model.HttpResponse
import com.sksamuel.elastic4s.Response
import com.sksamuel.elastic4s.requests.delete.DeleteResponse
import ru.misis.event.Cart.{CartId, Order}
import ru.misis.event.OrderStatuses.OrderStatus
import ru.misis.event.Payment.Cheque

import scala.concurrent.Future

trait CartCommands {
    def replaceMenu(menu: Menu): Future[Menu]

    def getMenu: Future[Menu]

    def updateCart(cartId: CartId, cartItem: CartItem): Future[Cart]

    def deleteCart(cartId: CartId): Future[Response[DeleteResponse]]

    def createOrder(cartId: CartId, status: OrderStatus): Future[Order]

    def payForOrder(order: Order): Future[HttpResponse]
}

object Objects {
    case class CartItem(menuItemId: ItemId, amount: Int)

    case class Cart(id: CartId, items: Seq[CartItem])
}
