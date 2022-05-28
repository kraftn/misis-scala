package ru.misis.event

import ru.misis.event.Menu.ItemId
import ru.misis.event.OrderStatuses.OrderStatus

object Cart {
    type CartId = String

    case class OrderItem(menuItemId: ItemId,
                         name: String,
                         price: Double,
                         amount: Int,
                         cookedAmount: Int)

    case class Order(cartId: CartId, items: Seq[OrderItem], status: OrderStatus)

    case class OrderFormed(order: Order) extends Event
}

object OrderStatuses {
    sealed abstract class OrderStatus(val status: String)

    case object NotPaidOrder extends OrderStatus("Не оплачен")
    case object PaidOrder extends OrderStatus("Оплачен")
    case object TakenOrder extends OrderStatus("Принят")
    case object CompletedOrder extends OrderStatus("Завершён")

    val orderStatuses = List(NotPaidOrder, PaidOrder, TakenOrder, CompletedOrder)

    object OrderStatus {
        def fromString(status: String): Option[OrderStatus] = orderStatuses.find(_.status == status)
    }
}
