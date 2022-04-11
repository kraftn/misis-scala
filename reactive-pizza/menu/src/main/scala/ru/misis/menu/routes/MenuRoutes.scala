package ru.misis.menu.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import ru.misis.menu.model.{Item, MenuService, Stage}
import spray.json.DefaultJsonProtocol._
import ru.misis.menu.model.ModelJsonFormats._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import io.scalaland.chimney.dsl._
import spray.json._

import java.util.UUID

class MenuRoutes(menuService: MenuService)(implicit val system: ActorSystem[_]){

    implicit val itemDTOJsonFormat = jsonFormat5(ItemDTO)

    val routes: Route =
    path("items") {
        get {
            onSuccess(menuService.listItems()) { items =>
                complete(items)
            }
        }
    } ~
    path("item") {
        (post & entity(as[ItemDTO])) { itemDTO =>
            val item = itemDTO.into[Item]
                .transform

            onSuccess(menuService.saveItem(item)) { performed =>
                complete(StatusCodes.Created, performed)
            }
        }
    } ~
    path("item" / Segment) { id =>
        get {
            rejectEmptyResponse {
                onSuccess(menuService.getItem(id)) { response =>
                    complete(response)
                }
            }
        }
    } ~
    path("find" / Segment) { name =>
        get {
            onSuccess(menuService.findItem(name)) { items =>
                complete((StatusCodes.OK, items))
            }
        }
    }
}


case class ItemDTO(name: String,
                   description: Option[String],
                   category: String,
                   price: Double,
                   routeCard: Seq[Stage])