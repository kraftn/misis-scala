package ru.misis.payment.model

import ru.misis.event.Cart.Order
import ru.misis.event.Payment.Cheque

import scala.concurrent.Future

trait PaymentCommands {
    def makePayment(order: Order): Future[Cheque]
}
