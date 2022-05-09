package ru.misis.payment.service

import akka.actor.ActorSystem
import ru.misis.event.Cart.Order
import ru.misis.event.Payment.{Cheque, PaymentMade}
import ru.misis.payment.model.PaymentCommands
import ru.misis.util.WithKafka
import ru.misis.event.EventJsonFormats._

import scala.concurrent.{ExecutionContext, Future}

class PaymentCommandImpl(implicit executionContext: ExecutionContext,
                         val system: ActorSystem)
    extends PaymentCommands
    with WithKafka {

    override def makePayment(order: Order): Future[Cheque] = Future {
        val worth = order.items.map(item => item.price * item.amount).sum
        val cheque = Cheque(order.cartId, isSuccessful = true, worth)
        publishEvent(PaymentMade(cheque))
        cheque
    }
}
