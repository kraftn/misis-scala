package ru.misis.menu.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import play.api.libs.json.{Format, Json}

import java.util.UUID
import scala.concurrent.Future
import scala.util.Try

trait MenuService {
    def listItems(): Future[Seq[Item]]
    def getItem(id: String): Future[Item]
    def findItem(query:String): Future[Seq[Item]]
    def saveItem(item: Item): Future[Item]
}

case class Category(id: String = UUID.randomUUID().toString,
                    name: String,
                    itemIds: Seq[String])

case class Item(id: Option[String] = Some(UUID.randomUUID().toString),
                name: String,
                description: Option[String],
                price: Double,
                routeCard: Seq[Stage])

object Item {
    implicit val format: Format[Item] = Json.format[Item]
    implicit val itemHitReader: HitReader[Item] = hit => Try(Json.parse(hit.sourceAsString).as[Item])
    implicit val itemIndexable: Indexable[Item] = item => Json.toJson(item).toString()

}

case class Stage(name: String,
                 description: Option[String],
                 products: Seq[Product])

object Stage {
    implicit val format: Format[Stage] = Json.format[Stage]
}

case class Product(name: String, amount: Double)

object Product {
    implicit val format: Format[Product] = Json.format[Product]
}

