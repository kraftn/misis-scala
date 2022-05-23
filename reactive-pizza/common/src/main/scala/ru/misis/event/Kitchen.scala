package ru.misis.event

import ru.misis.event.Order.KitchenItem
import spray.json._

object Kitchen {
    case class ItemCooked(kitchenItem: KitchenItem) extends Event
}
