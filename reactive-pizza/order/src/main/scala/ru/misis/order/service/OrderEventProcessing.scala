package ru.misis.order.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import org.slf4j.LoggerFactory
import ru.misis.event.Cart.OrderFormed
import ru.misis.event.Kitchen.ItemCooked
import ru.misis.event.Order.{ItemReadyForCooking, KitchenItem, OrderCompleted, OrderReadyForCooking, OrderSaved}
import ru.misis.event.KitchenItemStatuses.NotCookedItem
import ru.misis.event.OrderStatuses.CompletedOrder
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
        .mapAsync(1) { orderFormed =>
            logger.info(s"Order formed ${orderFormed.toJson.prettyPrint}")
            orderService.saveOrder(orderFormed.order)
        }
        .map(order => OrderSaved(order))
        .runWith(kafkaSink)

    kafkaSource[OrderReadyForCooking]
        .mapConcat { event =>
            logger.info(s"Order ready for cooking ${event.toJson.prettyPrint}")
            val order = event.order
            order.items.flatMap { orderItem =>
                val kitchenItem = KitchenItem(
                    cartId = order.cartId,
                    menuItemId = orderItem.menuItemId,
                    status = NotCookedItem
                )
                (1 to orderItem.amount).map(_ => kitchenItem)
            }
        }
        .map(kitchenItem => ItemReadyForCooking(kitchenItem))
        .runWith(kafkaSink)

    kafkaSource[ItemCooked]
        .mapAsync(1) { itemCooked =>
            logger.info(s"Item cooked ${itemCooked.toJson.prettyPrint}")
            val kitchenItem = itemCooked.kitchenItem
            orderService.updateCookedAmount(kitchenItem.cartId, kitchenItem.menuItemId)
        }
        .mapAsync(1) { order =>
            orderService.isOrderCompleted(order.cartId).map { isOrderCompleted =>
                isOrderCompleted -> order
            }
        }
        .filter { case (isOrderCompleted, _) => isOrderCompleted }
        .mapAsync(1) { case (_, order) =>
            orderService.updateOrderStatus(order.cartId, CompletedOrder).map { updatedOrder =>
                OrderCompleted(updatedOrder)
            }
        }
        .runWith(kafkaSink)

    kafkaSource[OrderCompleted]
        .wireTap(orderCompleted => logger.info(s"Order completed ${orderCompleted.toJson.prettyPrint}"))
        .runWith(Sink.ignore)
}
