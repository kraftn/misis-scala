package ru.misis.menu.service

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink}
import org.slf4j.LoggerFactory
import ru.misis.event.Menu._
import ru.misis.menu.model.MenuCommands
import ru.misis.menu.model.Objects._
import ru.misis.util.{StreamHelper, WithKafka}
import spray.json._

import scala.concurrent.ExecutionContext

class MenuEventProcessing (menuService: MenuCommands)
                          (implicit executionContext: ExecutionContext,
                           override val system: ActorSystem)
    extends WithKafka
        with StreamHelper {

    import ru.misis.event.EventJsonFormats._
    import ru.misis.menu.model.ModelJsonFormats._

    private val logger = LoggerFactory.getLogger(this.getClass)

    /*
        source ~> broadcast2 ~> createMenu     ~> kafkaSink[MenuCreated]
                             ~> createRouteMap ~> kafkaSink[RouteCardCreated]
     */
    kafkaSource[ItemsEvent]
        .runWith(broadcastSink2(
            Flow[ItemsEvent]
                .map { case ItemsEvent(items) => MenuCreated(menuService.createMenu(items)) }
                .to(kafkaSink),
            Flow[ItemsEvent]
                .map { case ItemsEvent(items) => RouteCardCreated(menuService.createRouteMap(items)) }
                .to(kafkaSink)
        ))

    kafkaSource[MenuCreated]
        .wireTap(value => logger.info(s"Menu created ${value.toJson.prettyPrint}"))
        .runWith(Sink.ignore)

    kafkaSource[RouteCardCreated]
        .wireTap(value => logger.info(s"RouteCard created ${value.toJson.prettyPrint}"))
        .runWith(Sink.ignore)
}
