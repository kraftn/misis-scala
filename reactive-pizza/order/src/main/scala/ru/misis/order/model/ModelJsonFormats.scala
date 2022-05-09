package ru.misis.order.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import spray.json.DefaultJsonProtocol._
import spray.json._
import ru.misis.event.Cart.Order
import ru.misis.event.EventJsonFormats._

import scala.util.Try

object ModelJsonFormats {
    implicit val orderHitReader: HitReader[Order] = hit => Try(hit.sourceAsString.parseJson.convertTo[Order])
    implicit val orderIndexable: Indexable[Order] = order => order.toJson.compactPrint
}
