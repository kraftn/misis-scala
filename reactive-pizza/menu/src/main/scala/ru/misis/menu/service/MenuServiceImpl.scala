package ru.misis.menu.service

import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, HitReader, Indexable, RequestSuccess}
import ru.misis.menu.model.{Item, MenuService}
import com.sksamuel.elastic4s.ElasticDsl._
import ru.misis.menu.model.ModelJsonFormats._

import scala.concurrent.{ExecutionContext, Future}

class MenuServiceImpl(elastic: ElasticClient)(implicit executionContext: ExecutionContext) extends MenuService {

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
                        nestedField("routeCard").properties(
                            textField("name"),
                            textField("description"),
                            nestedField("products").properties(
                                textField("name"),
                                doubleField("amount")
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
}
