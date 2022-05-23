package ru.misis.kitchen.service

import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.delete.DeleteByQueryResponse
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.task.CreateTaskResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import ru.misis.event.Kitchen.ItemCooked
import ru.misis.event.Menu.{ItemId, RouteItem, RouteStage}
import ru.misis.event.Order.KitchenItem
import ru.misis.event.EventJsonFormats._
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
                        intField("currentRouteStageNumber")
                    )
                )
            }
        }

    override def replaceRouteCards(routeCards: Seq[RouteItem]): Future[Seq[RouteItem]] =
        elastic.execute {
            deleteByQuery(routeCardIndex, matchAllQuery())
        }.flatMap {
            case _: RequestSuccess[Either[DeleteByQueryResponse, CreateTaskResponse]] =>
                Future.sequence(
                    routeCards.map { routeCard =>
                        elastic.execute {
                            indexInto(routeCardIndex).id(routeCard.itemId).doc(routeCard)
                        }.map {
                            case _: RequestSuccess[IndexResponse] => routeCard
                        }
                    }
                )
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

    override def executeNextRouteStage(kitchenItemId: ItemId): Future[Option[RouteStage]] =
        elastic.execute {
            get(kitchenItemIndex, kitchenItemId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val kitchenItem = results.result.to[KitchenItem]
                val currentStage = kitchenItem.currentRouteStageNumber

                elastic.execute {
                    get(routeCardIndex, kitchenItem.menuItemId)
                }.flatMap {
                    case results: RequestSuccess[GetResponse] =>
                        val routeStages = results.result.to[RouteItem].routeStages

                        if (currentStage < routeStages.size - 1)
                            incrementRouteStage(kitchenItemId).map(_ => Some(routeStages(currentStage + 1)))
                        else
                            incrementRouteStage(kitchenItemId).flatMap { updatedKitchenItem =>
                                publishEvent(ItemCooked(updatedKitchenItem)).map(_ => None)
                            }
                }
        }

    override def isItemReady(kitchenItemId: ItemId): Future[Boolean] =
        elastic.execute {
            get(kitchenItemIndex, kitchenItemId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val kitchenItem = results.result.to[KitchenItem]
                val currentStage = kitchenItem.currentRouteStageNumber

                elastic.execute {
                    get(routeCardIndex, kitchenItem.menuItemId)
                }.map {
                    case results: RequestSuccess[GetResponse] =>
                        val routeStages = results.result.to[RouteItem].routeStages
                        currentStage >= routeStages.size
                }
        }

    override protected def incrementRouteStage(kitchenItemId: ItemId): Future[KitchenItem] =
        elastic.execute {
            get(kitchenItemIndex, kitchenItemId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val kitchenItem = results.result.to[KitchenItem]
                val updatedKitchenItem = kitchenItem.copy(
                    currentRouteStageNumber = kitchenItem.currentRouteStageNumber + 1
                )

                elastic.execute {
                    updateById(kitchenItemIndex, kitchenItemId).doc(updatedKitchenItem)
                }.map {
                    case _: RequestSuccess[UpdateResponse] => updatedKitchenItem
                }
        }
}
