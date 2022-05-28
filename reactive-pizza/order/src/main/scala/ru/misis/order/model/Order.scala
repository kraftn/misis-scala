package ru.misis.order.model

import akka.Done
import ru.misis.event.Cart.{CartId, Order}
import ru.misis.event.Menu.ItemId
import ru.misis.event.OrderStatuses.OrderStatus

import scala.concurrent.Future

trait OrderCommands {
    def saveOrder(order: Order): Future[Order]

    def listOrders(): Future[Seq[Order]]

    def takeOrder(cartId: CartId): Future[Done]

    def isOrderCompleted(cartId: CartId): Future[Boolean]

    def getOrderStatus(cartId: CartId): Future[OrderStatus]

    def updateOrderStatus(cartId: CartId, status: OrderStatus): Future[Order]

    def updateCookedAmount(cartId: CartId, menuItemId: ItemId): Future[Order]
}
