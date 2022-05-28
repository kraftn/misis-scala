package ru.misis.event

import ru.misis.event.Cart.{CartId, Order}
import ru.misis.event.Menu.ItemId
import ru.misis.event.KitchenItemStatuses.KitchenItemStatus

import java.util.UUID

object Order {
    case class KitchenItem(id: ItemId = UUID.randomUUID().toString,
                           cartId: CartId,
                           menuItemId: ItemId,
                           status: KitchenItemStatus)

    case class OrderSaved(order: Order) extends Event

    case class OrderReadyForCooking(order: Order) extends Event

    case class ItemReadyForCooking(item: KitchenItem) extends Event

    case class OrderCompleted(order: Order) extends Event
}

object KitchenItemStatuses {
    sealed abstract class KitchenItemStatus(val status: String)

    case object NotCookedItem extends KitchenItemStatus("Не приготовлено")
    case object CookedItem extends KitchenItemStatus("Приготовлено")

    val orderItemStatuses = List(NotCookedItem, CookedItem)

    object KitchenItemStatus {
        def fromString(status: String): Option[KitchenItemStatus] = orderItemStatuses.find(_.status == status)
    }
}
