package ru.misis.event

import ru.misis.event.Cart._
import ru.misis.event.Menu._
import ru.misis.event.Order.{KitchenItem, OrderTaken}
import ru.misis.event.OrderStatuses.OrderStatus
import ru.misis.event.Payment.{Cheque, PaymentMade}
import spray.json._
import spray.json.DefaultJsonProtocol._

object EventJsonFormats {
    implicit val productJsonFormat = jsonFormat2(Product)
    implicit val stageJsonFormat = jsonFormat4(RouteStage)
    implicit val stageItemJsonFormat = jsonFormat2(RouteItem)
    implicit val menuItemJsonFormat = jsonFormat4(MenuItem)

    implicit val menuCategoryFormat = jsonFormat2(MenuCategory)
    implicit val menuFormat = jsonFormat1(Menu)

    implicit val orderItemFormat = jsonFormat4(OrderItem)
    implicit val orderStatusFormat = new RootJsonFormat[OrderStatus] {
        override def write(obj: OrderStatus): JsValue = JsString(obj.status)
        override def read(json: JsValue): OrderStatus = OrderStatus.fromString(json.convertTo[String]).get
    }
    implicit val orderFormat = jsonFormat3(Order)
    implicit val chequeFormat = jsonFormat3(Cheque)

    implicit val kitchenItemFormat = jsonFormat4(KitchenItem)

    implicit val menuCreatedFormat = jsonFormat1(MenuCreated)
    implicit val routeCardCreatedFormat = jsonFormat1(RouteCardCreated)

    implicit val orderFormedFormat = jsonFormat1(OrderFormed)
    implicit val paymentMadeFormat = jsonFormat1(PaymentMade)

    implicit val orderTakenFormat = jsonFormat1(OrderTaken)
}
