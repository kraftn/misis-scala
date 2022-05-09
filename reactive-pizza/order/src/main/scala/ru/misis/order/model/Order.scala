package ru.misis.order.model

import akka.Done
import ru.misis.event.Cart.{CartId, Order}

import scala.concurrent.Future

trait OrderCommands {
    def saveOrder(order: Order): Future[Order]

    def listOrders(): Future[Seq[Order]]

    def takeOrder(cartId: CartId): Future[Seq[Done]]
}
