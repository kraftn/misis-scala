package ru.misis.menu.service

import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.indexes.admin.IndexExistsResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.{ElasticClient, HitReader, Indexable, RequestSuccess}
import play.api.libs.json.{Format, Json}
import ru.misis.menu.model.{Item, MenuService}

import scala.concurrent.{ExecutionContext, Future}

class MenuServiceImpl(elastic: ElasticClient)(implicit executionContext: ExecutionContext) extends MenuService{

    import com.sksamuel.elastic4s.ElasticDsl._

    implicit val formatItem: Format[Item] = Json.format[Item]

    val itemIndex = "item"

    elastic.execute{
        indexExists(itemIndex)
    }.flatMap{
        case results: RequestSuccess[IndexExistsResponse] if !results.result.isExists =>
            elastic.execute {
                createIndex(itemIndex).mapping(
                    properties(
                        keywordField("id"),
                        textField("name").boost(4),
                        textField("description"),
                        doubleField("price")
                    )
                )
            }
        case _ => Future.unit
    }

    override def listItems(): Future[Seq[Item]] = {
        elastic.execute{
            search(itemIndex)
        }.map{
            case results: RequestSuccess[SearchResponse] => results.result.to[Item]
        }

    }

    override def getItem(id: String): Future[Item] =
        elastic.execute{
            get(itemIndex, id)
        }.map{
            case results: RequestSuccess[GetResponse] => results.result.to[Item]
        }

    override def findItem(query: String): Future[Seq[Item]] = ???

    override def saveItem(item: Item): Future[Item] =
        elastic.execute{
            indexInto(itemIndex).id(item.id.get).doc(item)
        }.map{
            case results: RequestSuccess[IndexResponse] => item
        }
}
