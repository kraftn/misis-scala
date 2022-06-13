package ru.misis.kitchen.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import spray.json._
import spray.json.DefaultJsonProtocol._
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Menu.RouteItem
import ru.misis.event.Order.KitchenItem
import ru.misis.kitchen.model.Objects.{StageDone, StageReadyForCooking}

import scala.util.Try

object ModelJsonFormats {
    implicit val routeItemHitReader: HitReader[RouteItem] = hit => Try(hit.sourceAsString.parseJson.convertTo[RouteItem])
    implicit val routeItemIndexable: Indexable[RouteItem] = routeItem => routeItem.toJson.compactPrint

    implicit val kitchenItemHitReader: HitReader[KitchenItem] = hit => Try(hit.sourceAsString.parseJson.convertTo[KitchenItem])
    implicit val kitchenItemIndexable: Indexable[KitchenItem] = item => item.toJson.compactPrint

    implicit val stageReadyForCookingFormat = jsonFormat3(StageReadyForCooking)
    implicit val stageDoneFormat = jsonFormat2(StageDone)
}
