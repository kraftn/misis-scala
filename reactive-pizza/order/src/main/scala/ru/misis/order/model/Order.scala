package ru.misis.order.model

import akka.Done
import com.sksamuel.elastic4s.Response
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import ru.misis.event.Cart.{CartId, Order}
import ru.misis.event.Order.KitchenItem
import ru.misis.event.OrderStatuses.OrderStatus

import scala.concurrent.Future

trait OrderCommands {
    def saveOrder(order: Order): Future[Order]

    def listOrders(): Future[Seq[Order]]

    def takeOrder(cartId: CartId): Future[Response[UpdateResponse]]

    def formOrder(cartId: CartId): Future[Response[UpdateResponse]]

    def completeOrder(cartId: CartId): Future[Response[UpdateResponse]]

    def getStatus(cartId: CartId): Future[OrderStatus]

    def pushToQueue(kitchenItem: KitchenItem): Future[KitchenItem]

    protected def changeStatus(cartId: CartId, status: OrderStatus): Future[Response[UpdateResponse]]
}
