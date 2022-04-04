package ru.misis.menu.routes

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json.JsObject
import ru.misis.menu.model.{Item, MenuService}

import java.util.UUID

class MenuRoutes(menuService: MenuService)(implicit val system: ActorSystem[_]) extends PlayJsonSupport{

    val routes: Route =
    path("items") {
        get {
            onSuccess(menuService.listItems()) { items =>
                complete(items)
            }
        }
    } ~
    path("item") {
        (post & entity(as[JsObject])) { json =>
            val id = (json \ "id").asOpt[String].getOrElse(UUID.randomUUID().toString)
            val value: JsObject = json + ("id" -> id)
            onSuccess(menuService.saveItem(value.as[Item])) { performed =>
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
    path("item" / Segment) { name =>
        delete {
            onSuccess(menuService.findItem(name)) { items =>
                complete((StatusCodes.OK, items))
            }
        }
    }
}
