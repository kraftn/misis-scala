package ru.misis.kitchen.service

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.bulk.BulkResponse
import com.sksamuel.elastic4s.requests.delete.DeleteByQueryResponse
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.task.CreateTaskResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.event.Menu.{ItemId, RouteItem, RouteStage}
import ru.misis.event.Order.KitchenItem
import ru.misis.event.EventJsonFormats._
import ru.misis.event.KitchenItemStatuses.KitchenItemStatus
import ru.misis.kitchen.model.KitchenCommands
import ru.misis.kitchen.model.ModelJsonFormats._
import ru.misis.util.WithKafka

import scala.concurrent.{ExecutionContext, Future}

class KitchenCommandImpl(elastic: ElasticClient)
                        (implicit executionContext: ExecutionContext,
                         val system: ActorSystem)
    extends KitchenCommands
    with WithKafka {

    val routeCardIndex = "route-card"
    val kitchenItemIndex = "kitchen-item"

    elastic.execute { deleteIndex(routeCardIndex) }
        .flatMap { _ =>
            elastic.execute {
                createIndex(routeCardIndex).mapping(
                    properties(
                        keywordField("itemId"),
                        nestedField("routeStages").properties(
                            textField("name"),
                            textField("description"),
                            nestedField("products").properties(
                                textField("name"),
                                intField("amount")
                            ),
                            intField("duration")
                        )
                    )
                )
            }
        }

    elastic.execute { deleteIndex(kitchenItemIndex) }
        .flatMap { _ =>
            elastic.execute {
                createIndex(kitchenItemIndex).mapping(
                    properties(
                        keywordField("id"),
                        keywordField("cartId"),
                        keywordField("menuItemId"),
                        keywordField("status")
                    )
                )
            }
        }

    override def replaceRouteCards(routeCards: Seq[RouteItem]): Future[Seq[RouteItem]] =
        elastic.execute {
            deleteByQuery(routeCardIndex, matchAllQuery())
        }.flatMap {
            case _: RequestSuccess[Either[DeleteByQueryResponse, CreateTaskResponse]] =>
                elastic.execute {
                    bulk(
                        routeCards.map { routeCard =>
                            indexInto(routeCardIndex).id(routeCard.itemId).doc(routeCard)
                        }
                    )
                }.map {
                    case _: RequestSuccess[BulkResponse] => routeCards
                }
        }

    override def saveItem(kitchenItem: KitchenItem): Future[KitchenItem] =
        elastic.execute {
            indexInto(kitchenItemIndex).id(kitchenItem.id).doc(kitchenItem)
        }.map {
            case _: RequestSuccess[IndexResponse] => kitchenItem
        }

    override def listItems(): Future[Seq[KitchenItem]] =
        elastic.execute {
            search(kitchenItemIndex).size(100)
        }.map {
            case results: RequestSuccess[SearchResponse] => results.result.to[KitchenItem]
        }

    override def getRouteStages(kitchenItemId: ItemId): Future[Seq[RouteStage]] = {
        elastic.execute {
            get(kitchenItemIndex, kitchenItemId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val kitchenItem = results.result.to[KitchenItem]
                elastic.execute {
                    get(routeCardIndex, kitchenItem.menuItemId)
                }.map {
                    case results: RequestSuccess[GetResponse] =>
                        results.result.to[RouteItem].routeStages
                }
        }
    }

    override def getItemStatus(kitchenItemId: ItemId): Future[KitchenItemStatus] =
        elastic.execute {
            get(kitchenItemIndex, kitchenItemId)
        }.map {
            case results: RequestSuccess[GetResponse] =>
                results.result.to[KitchenItem].status
        }

    override def updateItemStatus(kitchenItemId: ItemId, status: KitchenItemStatus): Future[KitchenItem] =
        elastic.execute {
            get(kitchenItemIndex, kitchenItemId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val updatedItem = results.result.to[KitchenItem].copy(status = status)
                elastic.execute {
                    updateById(kitchenItemIndex, kitchenItemId).doc(updatedItem)
                }.map {
                    case _: RequestSuccess[UpdateResponse] => updatedItem
                }
        }
}
