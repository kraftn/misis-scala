package ru.misis.kitchen.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import org.slf4j.LoggerFactory
import ru.misis.event.Menu._
import ru.misis.event.Order.OrderTaken
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.util.{StreamHelper, WithKafka}
import spray.json._

import scala.concurrent.{ExecutionContext, Future}

class KitchenEventProcessing(kitchenService: KitchenCommands)
                            (implicit executionContext: ExecutionContext,
                             override val system: ActorSystem)
    extends WithKafka
    with StreamHelper {

    import ru.misis.event.EventJsonFormats._
    import ru.misis.kitchen.model.ModelJsonFormats._

    private val logger = LoggerFactory.getLogger(this.getClass)

    kafkaSource[RouteCardCreated]
        .mapAsync(1) { routeCardCreated =>
            logger.info(s"RouteCardCreated created ${routeCardCreated.toJson.prettyPrint}")
            kitchenService.replaceRouteCards(routeCardCreated.routeCard)
        }
        .runWith(Sink.ignore)

    kafkaSource[OrderTaken]
        .mapAsync(1) { orderTaken =>
            logger.info(s"OrderTaken ${orderTaken.toJson.prettyPrint}")
            kitchenService.saveItem(orderTaken.item)
        }
        .runWith(Sink.ignore)
}
