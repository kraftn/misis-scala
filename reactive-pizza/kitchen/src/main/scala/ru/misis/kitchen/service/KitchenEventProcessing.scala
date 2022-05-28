package ru.misis.kitchen.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import org.slf4j.LoggerFactory
import ru.misis.event.Kitchen.ItemCooked
import ru.misis.event.KitchenItemStatuses.CookedItem
import ru.misis.event.Menu._
import ru.misis.event.Order.ItemReadyForCooking
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.util.{StreamHelper, WithKafka}
import spray.json._

import scala.concurrent.duration.DurationInt
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
            logger.info(s"RouteCard created ${routeCardCreated.toJson.prettyPrint}")
            kitchenService.replaceRouteCards(routeCardCreated.routeCard)
        }
        .runWith(Sink.ignore)

    kafkaSource[ItemReadyForCooking]
        .mapAsync(1) { itemReadyForCooking =>
            logger.info(s"Item ready for cooking ${itemReadyForCooking.toJson.prettyPrint}")
            val kitchenItem = itemReadyForCooking.item
            kitchenService.saveItem(kitchenItem)
        }
        .mapAsync(1) { kitchenItem =>
            kitchenService.getDuration(kitchenItem.menuItemId).map { duration =>
                Thread.sleep(duration.seconds.toMillis)
                kitchenItem
            }
        }
        .mapAsync(1) { kitchenItem =>
            kitchenService.updateItemStatus(kitchenItem.id, CookedItem).map { updatedKitchenItem =>
                ItemCooked(updatedKitchenItem)
            }
        }
        .runWith(kafkaSink)
}
