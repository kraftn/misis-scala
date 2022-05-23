package ru.misis.event

import ru.misis.event.Cart.{CartId, Order}
import ru.misis.event.Menu.ItemId

import java.util.UUID

object Order {
    case class KitchenItem(id: ItemId = UUID.randomUUID().toString,
                           cartId: CartId,
                           menuItemId: ItemId,
                           currentRouteStageNumber: Int)

    case class OrderTaken(item: KitchenItem) extends Event

    case class OrderCompleted(order: Order) extends Event
}
