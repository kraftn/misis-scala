package ru.misis.cart.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.slf4j.LoggerFactory
import ru.misis.cart.model.CartCommands
import ru.misis.event.Cart.OrderFormed
import ru.misis.event.Menu.MenuCreated
import ru.misis.event.OrderStatuses.PaidOrder
import ru.misis.event.Payment.{Cheque, PaymentMade}
import ru.misis.util.{StreamHelper, WithKafka}
import spray.json._

import scala.concurrent.ExecutionContext

class CartEventProcessing(cartService: CartCommands)
                         (implicit executionContext: ExecutionContext,
                          override val system: ActorSystem)
    extends WithKafka
    with StreamHelper {

    import ru.misis.event.EventJsonFormats._

    private val logger = LoggerFactory.getLogger(this.getClass)

    kafkaSource[MenuCreated]
        .mapAsync(1) { menuCreated =>
            logger.info(s"Menu created ${menuCreated.toJson.prettyPrint}")
            cartService.replaceMenu(menuCreated.menu)
        }
        .runWith(Sink.ignore)

    kafkaSource[PaymentMade]
        .wireTap(paymentMade => logger.info(s"Payment made ${paymentMade.toJson.prettyPrint}"))
        .filter(paymentMade => paymentMade.cheque.isSuccessful)
        .mapAsync(1) { case PaymentMade(Cheque(cartId, _, _)) =>
            for {
                order <- cartService.createOrder(cartId, PaidOrder)
                _ <- cartService.deleteCart(cartId)
            } yield OrderFormed(order)
        }
        .runWith(kafkaSink)
}
