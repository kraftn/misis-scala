package ru.misis.menu.model

import akka.Done
import ru.misis.event.Event
import ru.misis.event.Menu._
import ru.misis.menu.model.Objects._

import java.util.UUID
import scala.concurrent.Future

trait MenuCommands {
    def listItems(): Future[Seq[Item]]

    def getItem(id: String): Future[Item]

    def findItem(query: String): Future[Seq[Item]]

    def saveItem(item: Item): Future[Item]

    def publish(itemIds: Seq[String]): Future[Done]
}

object Objects {

    case class Item(id: ItemId = UUID.randomUUID().toString,
                    name: String,
                    category: String,
                    description: Option[String],
                    price: Double,
                    routeStages: Seq[RouteStage])

    case class ItemsEvent(items: Seq[Item]) extends Event
}
