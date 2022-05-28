package ru.misis.waiter_bot.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import org.slf4j.LoggerFactory
import ru.misis.event.Order.OrderSaved
import ru.misis.util.{StreamHelper, WithKafka}
import ru.misis.waiter_bot.model.WaiterCommands
import spray.json._

import scala.concurrent.ExecutionContext

class WaiterEventProcessing(waiterService: WaiterCommands)
                           (implicit executionContext: ExecutionContext,
                            override val system: ActorSystem)
    extends WithKafka
        with StreamHelper {

    import ru.misis.event.EventJsonFormats._

    private val logger = LoggerFactory.getLogger(this.getClass)

    kafkaSource[OrderSaved]
        .mapAsync(1) { orderSaved =>
            logger.info(s"Order saved ${orderSaved.toJson.prettyPrint}")
            waiterService.takeOrder(orderSaved.order.cartId)
        }
        .runWith(Sink.ignore)
}
