package ru.misis.waiter_bot.model

import akka.http.scaladsl.model.HttpResponse
import ru.misis.event.Cart.CartId

import scala.concurrent.Future

trait WaiterCommands {
    def takeOrder(cartId: CartId): Future[HttpResponse]
}
