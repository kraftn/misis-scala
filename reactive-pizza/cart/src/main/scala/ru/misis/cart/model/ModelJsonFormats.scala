package ru.misis.cart.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import Objects._
import spray.json._
import spray.json.DefaultJsonProtocol._
import ru.misis.event.EventJsonFormats._
import ru.misis.event.Menu.Menu

import scala.util.Try

object ModelJsonFormats {
    implicit val cartItemJsonFormat = jsonFormat2(CartItem)
    implicit val cartJsonFormat = jsonFormat2(Cart)

    implicit val cartHitReader: HitReader[Cart] = hit => Try(hit.sourceAsString.parseJson.convertTo[Cart])
    implicit val cartIndexable: Indexable[Cart] = cart => cart.toJson.compactPrint

    implicit val menuHitReader: HitReader[Menu] = hit => Try(hit.sourceAsString.parseJson.convertTo[Menu])
    implicit val menuIndexable: Indexable[Menu] = menu => menu.toJson.compactPrint
}
