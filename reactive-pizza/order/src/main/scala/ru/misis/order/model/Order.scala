package ru.misis.order.model

import akka.Done
import com.sksamuel.elastic4s.Response
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import ru.misis.event.Cart.{CartId, Order}

import scala.concurrent.Future

trait OrderCommands {
    def saveOrder(order: Order): Future[Order]

    def listOrders(): Future[Seq[Order]]

    def takeOrder(cartId: CartId): Future[Response[UpdateResponse]]
}
