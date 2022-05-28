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
import ru.misis.event.Menu.ItemId
import ru.misis.event.Order.OrderReadyForCooking
import ru.misis.event.OrderStatuses.{OrderStatus, TakenOrder}
import ru.misis.event.EventJsonFormats._
import ru.misis.util.WithKafka
import ru.misis.order.model.OrderCommands
import ru.misis.order.model.ModelJsonFormats._

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
                            intField("amount"),
                            intField("cookedAmount")
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

    override def takeOrder(cartId: CartId): Future[Done] =
        elastic.execute {
            get(orderIndex, cartId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                publishEvent(OrderReadyForCooking(results.result.to[Order]))
        }

    override def isOrderCompleted(cartId: CartId): Future[Boolean] =
        elastic.execute {
            get(orderIndex, cartId)
        }.map {
            case results: RequestSuccess[GetResponse] =>
                val order = results.result.to[Order]
                order.items.foldRight(true)((orderItem, acc) => acc && (orderItem.cookedAmount == orderItem.amount))
        }

    override def getOrderStatus(cartId: CartId): Future[OrderStatus] =
        elastic.execute {
            get(orderIndex, cartId)
        }.map {
            case results: RequestSuccess[GetResponse] => results.result.to[Order].status
        }

    override def updateOrderStatus(cartId: CartId, status: OrderStatus): Future[Order] = {
        elastic.execute {
            get(orderIndex, cartId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val updatedOrder = results.result.to[Order].copy(status = status)
                elastic.execute {
                    updateById(orderIndex, cartId).doc(updatedOrder)
                }.map {
                    case _: RequestSuccess[UpdateResponse] => updatedOrder
                }
        }
    }

    override def updateCookedAmount(cartId: CartId, menuItemId: ItemId): Future[Order] =
        elastic.execute {
            get(orderIndex, cartId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val order = results.result.to[Order]
                val itemToUpdate = order.items.find(_.menuItemId == menuItemId).get
                val updatedItem = itemToUpdate.copy(cookedAmount = itemToUpdate.cookedAmount + 1)
                val updatedItems = order.items.filterNot(_.menuItemId == menuItemId) :+ updatedItem
                val updatedOrder = order.copy(items = updatedItems)

                elastic.execute {
                    updateById(orderIndex, cartId).doc(updatedOrder)
                }.map {
                    case _: RequestSuccess[UpdateResponse] => updatedOrder
                }
        }
}
