package ru.misis.registry

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import ru.misis.model.{Item, Items}

object ItemRegistry {

    sealed trait Command
    case class GetItems(replyTo: ActorRef[Items]) extends Command
    case class CreateItem(item: Item, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetItem(name: String, replyTo: ActorRef[GetItemResponse]) extends Command
    case class DeleteItem(name: String, replyTo: ActorRef[ActionPerformed]) extends Command

    final case class GetItemResponse(maybeItem: Option[Item])
    final case class ActionPerformed(description: String)


    def apply(): Behavior[Command] = registry(Set.empty)

    private def registry(items: Set[Item]): Behavior[Command] =
        Behaviors.receiveMessage {
            case GetItems(replyTo) =>
                replyTo ! Items(items.toSeq)
                Behaviors.same
            case CreateItem(item, replyTo) =>
                replyTo ! ActionPerformed(s"Item ${item.name} created.")
                registry(items + item)
            case GetItem(name, replyTo) =>
                replyTo ! GetItemResponse(items.find(_.name == name))
                Behaviors.same
            case DeleteItem(name, replyTo) =>
                replyTo ! ActionPerformed(s"Item $name updated.")
                registry(items.filterNot(_.name == name))
        }
}
