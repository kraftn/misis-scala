package ru.misis.event

import ru.misis.event.Cart._
import ru.misis.event.Kitchen.ItemCooked
import ru.misis.event.Menu._
import ru.misis.event.Order.{ItemReadyForCooking, KitchenItem, OrderCompleted, OrderReadyForCooking, OrderSaved}
import ru.misis.event.KitchenItemStatuses.KitchenItemStatus
import ru.misis.event.OrderStatuses.OrderStatus
import ru.misis.event.Payment.{Cheque, PaymentMade}
import spray.json._
import spray.json.DefaultJsonProtocol._

import scala.util.Try

object EventJsonFormats {
    implicit val productJsonFormat = jsonFormat2(Product)
    implicit val stageJsonFormat = jsonFormat4(RouteStage)
    implicit val stageItemJsonFormat = jsonFormat2(RouteItem)
    implicit val menuItemJsonFormat = jsonFormat4(MenuItem)

    implicit val menuCategoryFormat = jsonFormat2(MenuCategory)
    implicit val menuFormat = jsonFormat1(Menu)

    implicit val orderItemFormat = jsonFormat5(OrderItem)
    implicit val orderStatusFormat = new RootJsonFormat[OrderStatus] {
        override def read(json: JsValue): OrderStatus = OrderStatus.fromString(json.convertTo[String]).get
        override def write(obj: OrderStatus): JsValue = JsString(obj.status)
    }
    implicit val orderFormat = jsonFormat3(Order)
    implicit val chequeFormat = jsonFormat3(Cheque)

    implicit val kitchenItemStatusFormat = new RootJsonFormat[KitchenItemStatus] {
        override def read(json: JsValue): KitchenItemStatus = KitchenItemStatus.fromString(json.convertTo[String]).get
        override def write(obj: KitchenItemStatus): JsValue = JsString(obj.status)
    }
    implicit val kitchenItemFormat = jsonFormat4(KitchenItem)

    implicit val menuCreatedFormat = jsonFormat1(MenuCreated)
    implicit val routeCardCreatedFormat = jsonFormat1(RouteCardCreated)

    implicit val orderFormedFormat = jsonFormat1(OrderFormed)
    implicit val paymentMadeFormat = jsonFormat1(PaymentMade)

    implicit val orderSavedFormat = jsonFormat1(OrderSaved)
    implicit val orderReadyForCookingFormat = jsonFormat1(OrderReadyForCooking)
    implicit val itemReadyForCookingFormat = jsonFormat1(ItemReadyForCooking)
    implicit val orderCompletedFormat = jsonFormat1(OrderCompleted)

    implicit val itemCookedFormat = jsonFormat1(ItemCooked)
}
