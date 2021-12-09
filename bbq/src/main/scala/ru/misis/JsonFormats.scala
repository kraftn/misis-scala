package ru.misis

import ru.misis.model.{Item, User, Users}
import ru.misis.registry.ItemRegistry.Items
import ru.misis.registry.MenuRegistry.{MenuDto, MenusDto}
import ru.misis.registry.{ItemRegistry, MenuRegistry, UserRegistry}

//#json-formats
import spray.json.DefaultJsonProtocol

object JsonFormats  {
  // import the default encoders for primitive types (Int, String, Lists etc)
  import DefaultJsonProtocol._

  implicit val userJsonFormat = jsonFormat3(User)
  implicit val usersJsonFormat = jsonFormat1(Users)
  //  implicit val menusJsonFormat = jsonFormat1(MenuDto)
  //  implicit val menusJsonFormat = jsonFormat1(MenusDto)

  implicit val actionPerformedJsonFormat = jsonFormat1(UserRegistry.ActionPerformed)

  implicit val menusJsonFormat = jsonFormat1(MenusDto)
  implicit val itemJsonFormat = jsonFormat3(Item)
  implicit val itemsJsonFormat = jsonFormat1(Items)
  implicit val menuJsonFormat = jsonFormat2(MenuDto)
  implicit val actionPerformedJsonFormat2 = jsonFormat1(MenuRegistry.ActionPerformed)
  implicit val actionPerformedJsonFormat3 = jsonFormat1(ItemRegistry.ActionPerformed)
}
//#json-formats
