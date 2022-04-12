package ru.misis.menu.model

import java.util.UUID
import scala.concurrent.Future

trait MenuService {
    def listItems(): Future[Seq[Item]]
    def getItem(id: String): Future[Item]
    def findItem(query:String): Future[Seq[Item]]
    def saveItem(item: Item): Future[Item]
}

case class Category(id: String = UUID.randomUUID().toString,
                    name: String,
                    itemIds: Seq[String])

case class Item(id: String = UUID.randomUUID().toString,
                name: String,
                category: String,
                description: Option[String],
                price: Double,
                routeCard: Seq[Stage])

case class Stage(name: String,
                 description: Option[String],
                 products: Seq[Product])

case class Product(name: String, amount: Double)

