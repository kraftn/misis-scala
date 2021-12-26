package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

case class Order(id: Int, menuItemId: String, userId: Int, quantity: Int, status: String)

trait OrderRepo {
    class OrderTable(tag: Tag) extends Table[Order](tag, "Order") {
        val id = column[Int]("id", O.PrimaryKey)
        val menuItemId = column[String]("menu_item_id")
        val userId = column[Int]("user_id")
        val quantity = column[Int]("quantity")
        val status = column[String]("status")

        def * = (
            id,
            menuItemId,
            userId,
            quantity,
            status
        ) <> ((Order.apply _).tupled, Order.unapply)
    }

    val orderTable = TableQuery[OrderTable]
}
