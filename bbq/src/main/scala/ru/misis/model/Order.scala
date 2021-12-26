package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

import java.util.UUID

case class Order(id: Int, userId: Int, status: String)
case class OrderItem(id: String = UUID.randomUUID().toString, orderId: Int, menuItemId: String, quantity: Int)

trait OrderRepo {
    class OrderTable(tag: Tag) extends Table[Order](tag, "Order") {
        val id = column[Int]("id", O.PrimaryKey)
        val userId = column[Int]("user_id")
        val status = column[String]("status")

        def * = (
            id,
            userId,
            status
        ) <> ((Order.apply _).tupled, Order.unapply)
    }

    class OrderItemTable(tag: Tag) extends Table[OrderItem](tag, "OrderItem") {
        val id = column[String]("id", O.PrimaryKey)
        val orderId = column[Int]("order_id")
        val menuItemId = column[String]("menu_item_id")
        val quantity = column[Int]("quantity")

        def * = (
            id,
            orderId,
            menuItemId,
            quantity
        ) <> ((OrderItem.apply _).tupled, OrderItem.unapply)
    }

    val orderTable = TableQuery[OrderTable]
    val orderItemTable = TableQuery[OrderItemTable]
}
