package ru.misis.kitchen.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import io.scalaland.chimney.dsl._
import ru.misis.event.EventJsonFormats._
import ru.misis.kitchen.model.KitchenCommands
import spray.json.DefaultJsonProtocol._
import spray.json._

import java.util.UUID

class KitchenRoutes(kitchenService: KitchenCommands)(implicit val system: ActorSystem) {

    val routes: Route =
    path("items") {
        get {
            onSuccess(kitchenService.listItems()) { items =>
                complete(items)
            }
        }
    } ~
    path("route-card" / Segment) { kitchenItemId =>
        get {
            onSuccess(kitchenService.getRouteStages(kitchenItemId)) { routeStages =>
                complete(routeStages)
            }
        }
    } ~
    path("status" / Segment) { kitchenItemId =>
        get {
            onSuccess(kitchenService.getItemStatus(kitchenItemId)) { status =>
                complete(status)
            }
        }
    }
}
