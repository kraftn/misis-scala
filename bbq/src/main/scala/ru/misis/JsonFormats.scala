package ru.misis

import ru.misis.model.{Item, Menu, User}
import ru.misis.registry.ItemRegistry.Items
import ru.misis.registry.MenuRegistry.{MenuDto, MenusDto}
import ru.misis.registry.OrderRegistry.{OrderDto, OrderItemDto, OrdersDto}
import ru.misis.registry.UserRegistry.Users
import ru.misis.registry.{ItemRegistry, MenuRegistry, OrderRegistry, UserRegistry}

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat4(User)
  implicit val usersJsonFormat = jsonFormat1(Users)
  //  implicit val menusJsonFormat = jsonFormat1(MenuDto)
  //  implicit val menusJsonFormat = jsonFormat1(MenusDto)

  implicit val actionPerformedJsonFormat = jsonFormat1(UserRegistry.ActionPerformed)

  implicit val menusDtoJsonFormat = jsonFormat1(MenusDto)
  implicit val itemJsonFormat = jsonFormat3(Item)
  implicit val itemsJsonFormat = jsonFormat1(Items)
  implicit val menuDtoJsonFormat = jsonFormat3(MenuDto)
  implicit val actionPerformedJsonFormat2 = jsonFormat1(MenuRegistry.ActionPerformed)
  implicit val actionPerformedJsonFormat3 = jsonFormat1(ItemRegistry.ActionPerformed)
  implicit val actionPerformedJsonFormat4 = jsonFormat1(OrderRegistry.ActionPerformed)

  implicit val menuJsonFormat = jsonFormat2(Menu)
  implicit val orderItemJsonFormat = jsonFormat4(OrderItemDto)
  implicit val orderJsonFormat = jsonFormat4(OrderDto)
  implicit val ordersJsonFormat = jsonFormat1(OrdersDto)
}
//#json-formats
