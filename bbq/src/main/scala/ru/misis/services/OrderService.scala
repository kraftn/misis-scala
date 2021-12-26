package ru.misis.services

import ru.misis.model.{ItemRepo, MenuItem, MenuRepo, Order, OrderItem, OrderRepo, UserRepo}
import ru.misis.registry.OrderRegistry.{OrderDto, OrderItemDto, OrdersDto}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait OrderService {
    def createOrder(order: OrderDto): Future[Unit]
    def changeStatus(id: Int, status: String): Future[Unit]
    def getOrders: Future[OrdersDto]
    def getOrdersByStatus(status: String): Future[OrdersDto]
    def getOrder(id: Int): Future[Option[OrderDto]]
    def updateOrder(id: Int, order: OrderDto): Future[Unit]
    def deleteOrder(id: Int): Future[Unit]
}

trait OrderServiceImpl extends OrderService with OrderRepo with UserRepo with MenuRepo with ItemRepo {
    def db: Database
    implicit def executionContext: ExecutionContext

    override def createOrder(orderDto: OrderDto): Future[Unit] = {
        Future.sequence(formOrderItems(orderDto)).map { maybeOrderItems =>
            db.run {
                DBIO.seq(
                    orderTable += Order(orderDto.id, orderDto.user.id, orderDto.status),
                    orderItemTable ++= maybeOrderItems.flatten
                ).transactionally
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
                .join(orderItemTable).on{ case ((order, _), orderItem) => order.id === orderItem.orderId }
                .join(menuItemTable).on{ case ((_, orderItem), menuItem) => orderItem.menuItemId === menuItem.id }
                .join(menuTable).on{ case ((_, menuItem), menu) => menuItem.menuId === menu.id }
                .join(itemTable).on{ case (((_, menuItem), menu), item) => menuItem.itemId === item.id }
                .result
        }.map { seq =>
            seq.map { case (((((order, user), orderItem), menuItem), menu), item) =>
                (order, user, OrderItemDto(menu, item, menuItem.price, orderItem.quantity))
            }.groupBy { case (order, user, _) =>
                (order, user)
            }.map { case ((order, user), seq) =>
                OrderDto(order.id, user, order.status, seq.map { case (_, _, orderItem) => orderItem })
            }.toSeq
        }.map(OrdersDto)
    }

    override def getOrdersByStatus(status: String): Future[OrdersDto] = {
        getOrders.map(ordersDto => OrdersDto(ordersDto.orders.filter(orderDto => orderDto.status == status)))
    }

    override def getOrder(id: Int): Future[Option[OrderDto]] = {
        getOrders.map(ordersDto => ordersDto.orders.find(orderDto => orderDto.id == id))
    }

    override def updateOrder(id: Int, orderDto: OrderDto): Future[Unit] = {
        Future.sequence(formOrderItems(orderDto)).map { maybeOrderItems =>
            db.run {
                orderTable.filter(_.id === id).update(Order(orderDto.id, orderDto.user.id, orderDto.status)).map { nRows =>
                    if (nRows > 0) {
                        db.run {
                            DBIO.seq(
                                orderItemTable.filter(_.orderId === id).delete,
                                orderItemTable ++= maybeOrderItems.flatten
                            )
                        }
                    }
                }
            }
        }
    }

    override def deleteOrder(id: Int): Future[Unit] = {
        db.run {
            DBIO.seq(
                orderItemTable.filter(_.orderId === id).delete,
                orderTable.filter(_.id === id).delete
            )
        }
    }

    private def formOrderItems(orderDto: OrderDto): Seq[Future[Option[OrderItem]]] = {
        orderDto.orderItems.map { orderItemDto =>
            db.run {
                menuItemTable.filter { menuItem =>
                    menuItem.menuId === orderItemDto.menu.id && menuItem.itemId === orderItemDto.item.id
                }.result.headOption
            }.map { maybeMenuItem =>
                maybeMenuItem.map { menuItem =>
                    OrderItem(
                        orderId = orderDto.id,
                        menuItemId = menuItem.id,
                        quantity = orderItemDto.quantity)
                }
            }
        }
    }
}
