package ru.misis.menu.model

import com.sksamuel.elastic4s.{HitReader, Indexable}
import spray.json._

import scala.util.Try

object ModelJsonFormats {
    // import the default encoders for primitive types (Int, String, Lists etc)

    import DefaultJsonProtocol._

    implicit val productJsonFormat = jsonFormat2(Product)
    implicit val stageJsonFormat = jsonFormat3(Stage)
    implicit val itemJsonFormat = jsonFormat6(Item)

    implicit val itemHitReader: HitReader[Item] = hit => Try(hit.sourceAsString.parseJson.convertTo[Item])
    implicit val itemIndexable: Indexable[Item] = item => item.toJson.compactPrint

}
