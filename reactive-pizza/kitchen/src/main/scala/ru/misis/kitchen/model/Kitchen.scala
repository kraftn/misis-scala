package ru.misis.kitchen.model

import ru.misis.event.Event
import ru.misis.event.KitchenItemStatuses.KitchenItemStatus
import ru.misis.event.Menu._
import ru.misis.event.Order.KitchenItem

import scala.concurrent.Future

trait KitchenCommands {
    def replaceRouteCards(routeCards: Seq[RouteItem]): Future[Seq[RouteItem]]

    def saveItem(kitchenItem: KitchenItem): Future[KitchenItem]

    def listItems(): Future[Seq[KitchenItem]]

    def getRouteStages(kitchenItemId: ItemId): Future[Seq[RouteStage]]

    def getItemStatus(kitchenItemId: ItemId): Future[KitchenItemStatus]

    def updateItemStatus(kitchenItemId: ItemId, status: KitchenItemStatus): Future[KitchenItem]
}

object Objects {
    case class StageReadyForCooking(item: KitchenItem,
                                    stage: RouteStage,
                                    isStageFinal: Boolean) extends Event
    case class StageDone(item: KitchenItem, isStageFinal: Boolean) extends Event
}
