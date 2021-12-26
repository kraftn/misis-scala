package ru.misis.services

import ru.misis.model.{ItemRepo, MenuItem, MenuRepo, Order, OrderRepo, UserRepo}
import ru.misis.registry.OrderRegistry.{OrderDto, OrdersDto}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait OrderService {
    def createOrder(order: OrderDto): Future[Option[Future[Int]]]
    def changeStatus(id: Int, status: String): Future[Unit]
    def getOrders: Future[OrdersDto]
    def getOrdersByStatus(status: String): Future[OrdersDto]
    def getOrder(id: Int): Future[Option[OrderDto]]
    def updateOrder(id: Int, order: OrderDto): Future[Option[Future[Int]]]
    def deleteOrder(id: Int): Future[Int]
}

trait OrderServiceImpl extends OrderService with OrderRepo with UserRepo with MenuRepo with ItemRepo {
    def db: Database
    implicit def executionContext: ExecutionContext

    override def createOrder(orderDto: OrderDto): Future[Option[Future[Int]]] = {
        fromOrderDto(orderDto).map { maybe =>
            maybe.map { order =>
                db.run {
                    orderTable += order
                }
            }
        }
    }

    override def changeStatus(id: Int, status: String): Future[Unit] = {
        getOrder(id).map(maybeOrder => maybeOrder.map(orderDto => updateOrder(id, orderDto.copy(status=status))))
    }

    override def getOrders: Future[OrdersDto] = {
        db.run {
            orderTable
                .join(userTable).on{ case (order, user) => order.userId === user.id }
                .join(menuItemTable).on{ case ((order, user), menuItem) => order.menuItemId === menuItem.id }
                .join(menuTable).on{ case ((_, menuitem), menu) => menuitem.menuId === menu.id }
                .join(itemTable).on{ case (((_, menuItem), menu), item) => menuItem.itemId === item.id }
                .result
        }.map(seq => seq.map {
            case ((((order, user), menuItem), menu), item) =>
                OrderDto(order.id, item, menu, menuItem.price, user, order.quantity, order.status)
        }).map(OrdersDto)
    }

    override def getOrdersByStatus(status: String): Future[OrdersDto] = {
        getOrders.map(ordersDto => OrdersDto(ordersDto.orders.filter(orderDto => orderDto.status == status)))
    }

    override def getOrder(id: Int): Future[Option[OrderDto]] = {
        getOrders.map(ordersDto => ordersDto.orders.find(orderDto => orderDto.orderId == id))
    }

    def updateOrder(id: Int, orderDto: OrderDto): Future[Option[Future[Int]]] = {
        fromOrderDto(orderDto).map { maybe =>
            maybe.map { order =>
                db.run(orderTable.filter(_.id === id).update(order))
            }
        }
    }

    override def deleteOrder(id: Int): Future[Int] = {
        db.run(orderTable.filter(_.id === id).delete)
    }

    private def fromOrderDto(orderDto: OrderDto): Future[Option[Order]] = {
        db.run {
            menuItemTable.filter {
                menuItem => menuItem.menuId === orderDto.menu.id && menuItem.itemId === orderDto.item.id
            }.result.headOption
        }.map(maybe => maybe.map { menuItem =>
            Order(orderDto.orderId, menuItem.id, orderDto.user.id, orderDto.quantity, orderDto.status)
        })
    }
}
