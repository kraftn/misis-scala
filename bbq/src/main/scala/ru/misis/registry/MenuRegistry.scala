package ru.misis.registry

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import ru.misis.model.{Item, Items, Menu, Menus}
import ru.misis.registry.ItemRegistry.{CreateItem, GetItem}


import scala.concurrent.{ExecutionContext, Future}


class MenuRegistry(itemRegistry: ActorRef[ItemRegistry.Command])(implicit val system: ActorSystem[_], executionContext: ExecutionContext){
    import MenuRegistry._
    private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

    def apply(): Behavior[Command] = registry(Set.empty)

    private def registry(menus: Set[Menu]): Behavior[Command] =
        Behaviors.receiveMessage {
            case GetMenus(replyTo) =>
                val result = menus.map(_.name)
                replyTo ! MenusDto(result.toSeq)
                Behaviors.same
            case CreateMenu(menuDto, replyTo) =>
                for {
                    item <- menuDto.items.items
                } yield itemRegistry.ask(CreateItem(item, _))
                replyTo ! ActionPerformed(s"Menu ${menuDto.name} created.")
                registry(menus + menuDto.toMenu)
            case GetMenu(name, replyTo) =>
                menus.find(_.name == name).map { menu =>
                    Future
                        .sequence(for {
                                itemName <- menu.items
                            } yield itemRegistry.ask(GetItem(itemName, _)))
                        .map{ maybeItems =>
                            val items = maybeItems.flatMap(_.maybeItem)
                            replyTo ! GetMenuResponse(Some(MenuDto(name, Items(items))))
                        }
                }
                Behaviors.same
            case DeleteMenu(name, replyTo) =>
                replyTo ! ActionPerformed(s"Menu $name updated.")
                registry(menus.filterNot(_.name == name))
        }
}

object MenuRegistry {
    sealed trait Command
    case class GetMenus(replyTo: ActorRef[MenusDto]) extends Command
    case class CreateMenu(menu: MenuDto, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetMenu(name: String, replyTo: ActorRef[GetMenuResponse]) extends Command
    case class DeleteMenu(name: String, replyTo: ActorRef[ActionPerformed]) extends Command


    case class MenusDto(names: Seq[String])
    case class MenuDto(name: String, items: Items){
        def toMenu: Menu = Menu(name, items.items.map(_.name))
    }
    implicit def toMenu(dto: MenuDto): Menu = Menu(dto.name, dto.items.items.map(_.name))


    final case class GetMenuResponse(maybe: Option[MenuDto])
    final case class ActionPerformed(description: String)
}
