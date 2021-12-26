package ru.misis.registry

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import ru.misis.model.{Item, Menu, User}
import ru.misis.services.OrderService

import scala.concurrent.ExecutionContext

abstract class OrderRegistry (implicit val system: ActorSystem[_], executionContext: ExecutionContext)
    extends OrderService {

    import OrderRegistry._
    private implicit val timeout = Timeout.create(system.settings.config.getDuration("my-app.routes.ask-timeout"))

    def apply(): Behavior[Command] =  Behaviors.receiveMessage {
        case CreateOrder(order, replyTo) =>
            val orderId = order.id
            createOrder(order).map(_ => replyTo ! ActionPerformed(s"Order №$orderId created."))
            Behaviors.same
        case ChangeStatus(id, status, replyTo) =>
            changeStatus(id, status).map(_ => replyTo ! ActionPerformed(s"Status has been changed to $status."))
            Behaviors.same
        case GetOrders(replyTo) =>
            getOrders.map(replyTo ! _)
            Behaviors.same
        case GetOrdersByStatus(status, replyTo) =>
            getOrdersByStatus(status).map(replyTo ! _)
            Behaviors.same
        case GetOrder(id, replyTo) =>
            getOrder(id).map(replyTo ! GetOrderResponse(_))
            Behaviors.same
        case UpdateOrder(id, order, replyTo) =>
            updateOrder(id, order).map(_ => replyTo ! ActionPerformed(s"Order №$id updated."))
            Behaviors.same
        case DeleteOrder(id, replyTo) =>
            deleteOrder(id).map(_ => replyTo ! ActionPerformed(s"Order №$id deleted."))
            Behaviors.same
    }
}

object OrderRegistry {
    sealed trait Command
    case class CreateOrder(order: OrderDto, replyTo: ActorRef[ActionPerformed]) extends Command
    case class ChangeStatus(id: Int, status: String, replyTo: ActorRef[ActionPerformed]) extends Command
    case class GetOrders(replyTo: ActorRef[OrdersDto]) extends Command
    case class GetOrdersByStatus(status: String, replyTo: ActorRef[OrdersDto]) extends Command
    case class GetOrder(id: Int, replyTo: ActorRef[GetOrderResponse]) extends Command
    case class UpdateOrder(id: Int, order: OrderDto, replyTo: ActorRef[ActionPerformed]) extends Command
    case class DeleteOrder(id: Int, replyTo: ActorRef[ActionPerformed]) extends Command

    case class OrderItemDto(menu: Menu, item: Item, menuPrice: Double, quantity: Int)
    case class OrderDto(id: Int, user: User, status: String, orderItems: Seq[OrderItemDto])
    case class OrdersDto(orders: Seq[OrderDto])

    final case class GetOrderResponse(maybe: Option[OrderDto])
    final case class ActionPerformed(description: String)
}
