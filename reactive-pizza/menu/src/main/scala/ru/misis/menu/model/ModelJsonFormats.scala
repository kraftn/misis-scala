package ru.misis.menu.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import ru.misis.menu.model.Objects._
import spray.json._
import spray.json.DefaultJsonProtocol._
import ru.misis.event.EventJsonFormats._

import scala.util.Try

object ModelJsonFormats {
    implicit val itemJsonFormat = jsonFormat6(Item)

    implicit val itemHitReader: HitReader[Item] = hit => Try(hit.sourceAsString.parseJson.convertTo[Item])
    implicit val itemIndexable: Indexable[Item] = item => item.toJson.compactPrint

    implicit val itemsEventFormat = jsonFormat1(ItemsEvent)
}
