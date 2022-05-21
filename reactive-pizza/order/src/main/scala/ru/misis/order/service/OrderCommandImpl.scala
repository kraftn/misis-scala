package ru.misis.order.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess, Response}
import ru.misis.event.Cart.{CartId, Order}
import ru.misis.event.Order.{KitchenItem, OrderTaken}
import ru.misis.order.model.OrderCommands
import ru.misis.util.WithKafka
import ru.misis.order.model.ModelJsonFormats._
import ru.misis.event.EventJsonFormats._
import ru.misis.event.OrderStatuses.TakenOrder

import scala.concurrent.{ExecutionContext, Future}

class OrderCommandImpl(elastic: ElasticClient)(implicit executionContext: ExecutionContext,
                                               val system: ActorSystem)
    extends OrderCommands
    with WithKafka {

    val orderIndex = "order"

    elastic.execute { deleteIndex(orderIndex) }
        .flatMap { _ =>
            elastic.execute {
                createIndex(orderIndex).mapping(
                    properties(
                        keywordField("cartId"),
                        nestedField("items").properties(
                            keywordField("menuItemId"),
                            textField("name"),
                            doubleField("price"),
                            intField("amount")
                        ),
                        keywordField("status")
                    )
                )
            }
        }

    override def saveOrder(order: Order): Future[Order] =
        elastic.execute {
            indexInto(orderIndex).id(order.cartId).doc(order)
        }.map {
            case _: RequestSuccess[IndexResponse] => order
        }

    override def listOrders(): Future[Seq[Order]] =
        elastic.execute(search(orderIndex))
            .map {
                case results: RequestSuccess[SearchResponse] => results.result.to[Order]
            }

    override def takeOrder(cartId: CartId): Future[Response[UpdateResponse]] =
        elastic.execute {
            get(orderIndex, cartId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val order = results.result.to[Order]

                val orderItems = order.items.flatMap(orderItem => (1 to orderItem.amount).map(_ => orderItem))
                val kitchenItems = orderItems.map { orderItem =>
                    KitchenItem(
                        cartId = order.cartId,
                        menuItemId = orderItem.menuItemId,
                        currentRouteStageNumber = -1
                    )
                }

                Future.sequence(kitchenItems.map(item => publishEvent(OrderTaken(item)))).flatMap { _ =>
                    elastic.execute {
                        updateById(orderIndex, order.cartId).doc(order.copy(status = TakenOrder))
                    }
                }
        }
}
