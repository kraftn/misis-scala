package ru.misis.event

import ru.misis.event.Cart.CartId

object Payment {
    case class Cheque(cartId: CartId, isSuccessful: Boolean, totalWorth: Double)

    case class PaymentMade(cheque: Cheque) extends Event
}
