package ru.misis.kitchen.model

import ru.misis.event.Menu._
import ru.misis.event.Order.KitchenItem

import scala.concurrent.Future

trait KitchenCommands {
    def replaceRouteCards(routeCards: Seq[RouteItem]): Future[Seq[RouteItem]]

    def saveItem(kitchenItem: KitchenItem): Future[KitchenItem]

    def listItems(): Future[Seq[KitchenItem]]

    def executeNextRouteStage(kitchenItemId: ItemId): Future[Option[RouteStage]]

    def isItemReady(kitchenItemId: ItemId): Future[Boolean]

    protected def incrementRouteStage(kitchenItemId: ItemId): Future[KitchenItem]
}
