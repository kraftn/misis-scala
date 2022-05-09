package ru.misis.menu.service

import akka.Done
import akka.actor.ActorSystem
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess}
import io.scalaland.chimney.dsl.TransformerOps
import ru.misis.event.Menu.{Menu, MenuCategory, MenuItem, RouteItem}
import ru.misis.menu.model.MenuCommands
import ru.misis.menu.model.ModelJsonFormats._
import ru.misis.menu.model.Objects._
import ru.misis.util.WithKafka

import scala.concurrent.{ExecutionContext, Future}

class MenuCommandImpl(elastic: ElasticClient)
                     (implicit executionContext: ExecutionContext,
                      val system: ActorSystem)
    extends MenuCommands
    with WithKafka {

    val itemIndex = "item"

    elastic.execute { deleteIndex(itemIndex) }
        .flatMap { _ =>
            elastic.execute {
                createIndex(itemIndex).mapping(
                    properties(
                        keywordField("id"),
                        textField("name").boost(4).analyzer("russian"),
                        textField("category"),
                        textField("description").analyzer("russian"),
                        doubleField("price"),
                        nestedField("routeStages").properties(
                            textField("name"),
                            textField("description"),
                            intField("duration"),
                            nestedField("products").properties(
                                textField("name"),
                                intField("amount")
                            )
                        )
                    )
                )
            }
        }

    override def listItems(): Future[Seq[Item]] = {
        elastic.execute(search(itemIndex))
            .map {
                case results: RequestSuccess[SearchResponse] => results.result.to[Item]
            }

    }

    override def getItem(id: String): Future[Item] =
        elastic.execute(get(itemIndex, id))
            .map {
                case results: RequestSuccess[GetResponse] => results.result.to[Item]
            }

    override def findItem(name: String): Future[Seq[Item]] = {
        elastic.execute {
                search(itemIndex).query(
                    should(
                        matchQuery("name", name),
                        matchQuery("description", name)
                    )
                )
            }
            .map{
                case results: RequestSuccess[SearchResponse] => results.result.to[Item]
            }
    }

    override def saveItem(item: Item): Future[Item] =
        elastic.execute { indexInto(itemIndex).id(item.id).doc(item) }
            .map{
                case results: RequestSuccess[IndexResponse] => item
            }

    override def publish(itemIds: Seq[String]): Future[Done] = {
        for {
            items <- Future.sequence(itemIds.map { itemId =>
                elastic.execute(get(itemIndex, itemId))
                    .map {
                        case results: RequestSuccess[GetResponse] => results.result.to[Item]
                    }
            })
            result <- publishEvent(ItemsEvent(items))
        } yield result
    }

    override def createMenu(items: Seq[Item]): Menu = {
        val categories = items
            .groupBy(_.category)
            .map { case (name, items) =>
                MenuCategory(name, items.map(item => item.into[MenuItem].transform))
            }
            .toSeq
        Menu(categories)
    }

    override def createRouteMap(items: Seq[Item]): Seq[RouteItem] = {
        items.map(item => RouteItem(item.id, item.routeStages))
    }
}
