package ru.misis.order.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import org.slf4j.LoggerFactory
import ru.misis.event.Cart.OrderFormed
import ru.misis.event.Order.OrderTaken
import ru.misis.order.model.OrderCommands
import ru.misis.util.{StreamHelper, WithKafka}
import spray.json._

import scala.concurrent.ExecutionContext

class OrderEventProcessing(orderService: OrderCommands)
                          (implicit executionContext: ExecutionContext,
                           override val system: ActorSystem)
    extends WithKafka
    with StreamHelper {

    import ru.misis.event.EventJsonFormats._

    private val logger = LoggerFactory.getLogger(this.getClass)

    kafkaSource[OrderFormed]
        .wireTap { orderFormed =>
            logger.info(s"Order formed ${orderFormed.toJson.prettyPrint}")
            orderService.saveOrder(orderFormed.order)
        }
        .runWith(Sink.ignore)

    kafkaSource[OrderTaken]
        .wireTap(orderTaken => logger.info(s"Order taken ${orderTaken.toJson.prettyPrint}"))
        .runWith(Sink.ignore)
}
