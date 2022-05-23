package ru.misis.order.service

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.delete.DeleteByQueryResponse
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.task.CreateTaskResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess, Response}
import ru.misis.event.Cart.{CartId, Order}
import ru.misis.event.Order.{KitchenItem, OrderCompleted, OrderTaken}
import ru.misis.event.EventJsonFormats._
import ru.misis.event.OrderStatuses.{CompletedOrder, CookedOrder, FormedOrder, OrderStatus, TakenOrder}
import ru.misis.util.WithKafka
import ru.misis.order.model.OrderCommands
import ru.misis.order.model.ModelJsonFormats._

import scala.concurrent.{ExecutionContext, Future}

class OrderCommandImpl(elastic: ElasticClient)(implicit executionContext: ExecutionContext,
                                               val system: ActorSystem)
    extends OrderCommands
    with WithKafka {

    val orderIndex = "order"
    val itemsQueueIndex = "items-queue"

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

    elastic.execute { deleteIndex(itemsQueueIndex) }
        .flatMap { _ =>
            elastic.execute {
                createIndex(itemsQueueIndex).mapping(
                    properties(
                        keywordField("id"),
                        keywordField("cartId"),
                        keywordField("menuItemId"),
                        intField("currentRouteStageNumber")
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
                    changeStatus(cartId, TakenOrder)
                }
        }

    override def formOrder(cartId: CartId): Future[Response[UpdateResponse]] = {
        elastic.execute {
            deleteByQuery(itemsQueueIndex, termQuery("cartId", cartId))
        }.flatMap {
            case _: RequestSuccess[Either[DeleteByQueryResponse, CreateTaskResponse]] =>
                changeStatus(cartId, FormedOrder)
        }
    }

    override def completeOrder(cartId: CartId): Future[Response[UpdateResponse]] =
        elastic.execute {
            get(orderIndex, cartId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val order = results.result.to[Order]
                publishEvent(OrderCompleted(order)).flatMap { _ =>
                    changeStatus(cartId, CompletedOrder)
                }
        }

    override def getStatus(cartId: CartId): Future[OrderStatus] =
        elastic.execute {
            get(orderIndex, cartId)
        }.map {
            case results: RequestSuccess[GetResponse] => results.result.to[Order].status
        }

    override def pushToQueue(kitchenItem: KitchenItem): Future[KitchenItem] =
        elastic.execute {
            indexInto(itemsQueueIndex).id(kitchenItem.id).doc(kitchenItem)
        }.flatMap {
            case _: RequestSuccess[IndexResponse] =>
                elastic.execute {
                    search(itemsQueueIndex).query(
                        termQuery("cartId", kitchenItem.cartId)
                    )
                }.flatMap {
                    case results: RequestSuccess[SearchResponse] =>
                        val cookedItems = results.result.to[KitchenItem].groupBy(_.menuItemId).map {
                            case (menuItemId, items) => menuItemId -> items.size
                        }
                        elastic.execute {
                            get(orderIndex, kitchenItem.cartId)
                        }.flatMap {
                            case results: RequestSuccess[GetResponse] =>
                                val order = results.result.to[Order]
                                val items = Map.from(order.items.map(item => item.menuItemId -> item.amount))
                                if (cookedItems == items)
                                    changeStatus(order.cartId, CookedOrder).map(_ => kitchenItem)
                                else
                                    Future(kitchenItem)
                        }
                }
        }

    override protected def changeStatus(cartId: CartId, status: OrderStatus): Future[Response[UpdateResponse]] =
        elastic.execute {
            updateById(orderIndex, cartId).doc("status" -> status.status)
        }
}
