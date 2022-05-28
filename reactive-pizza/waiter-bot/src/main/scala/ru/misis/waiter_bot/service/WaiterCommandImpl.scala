package ru.misis.waiter_bot.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpMethods, HttpRequest, HttpResponse}
import ru.misis.event.Cart.CartId
import ru.misis.util.WithKafka
import ru.misis.waiter_bot.model.WaiterCommands

import scala.concurrent.{ExecutionContext, Future}

class WaiterCommandImpl(implicit executionContext: ExecutionContext,
                        val system: ActorSystem)
    extends WaiterCommands
        with WithKafka {

    override def takeOrder(cartId: CartId): Future[HttpResponse] = {
        val request = HttpRequest(
            method = HttpMethods.POST,
            uri = s"${sys.env("ORDER_HTTP")}/take/$cartId",
        )
        Http().singleRequest(request)
    }
}
