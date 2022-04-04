package ru.misis.services

import ru.misis.model.{ItemRepo, Menu, MenuItem, MenuRepo}
import ru.misis.registry.MenuRegistry.{MenuDto, MenusDto}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}

trait MenuService {
    def getMenus(): Future[MenusDto]
    def createMenu(menuDto: MenuDto): Future[Unit]
    def getMenu(name: String): Future[Option[MenuDto]]
    def deleteMenu(name: String): Future[Unit]
}

trait MenuServiceImpl extends MenuService with MenuRepo with ItemRepo{
    def db: Database
    implicit def executionContext: ExecutionContext

    override def getMenus(): Future[MenusDto] = {
        db.run{
            menuTable.result.map(menus => MenusDto(menus.map(_.name)))
        }
    }

    override def createMenu(menu: MenuDto): Future[Unit] = {
        for {
            existedItems <- db.run(itemTable.filter(_.id inSet menu.items.map(_.id)).map(_.id).result)
            newItems = menu.items.filterNot(item => existedItems.contains(item.id))
            existedMenu <- db.run(menuTable.filter(_.id === menu.id).map(_.id).result.headOption)

            addItem = Some(itemTable ++= newItems)
            addMenu = existedMenu.map(_ => None).getOrElse(Some(menuTable += Menu(menu.id, menu.name)))
            addMenuItems = Some(menuItemTable ++= menu.items.map(item => MenuItem(
                menuId = menu.id,
                itemId = item.id,
                price = item.price)))
            actions = Seq(addItem, addMenu, addMenuItems).flatten
        } yield
            db.run {
                DBIO.sequence(actions).transactionally
            }
    }

    override def getMenu(name: String): Future[Option[MenuDto]] = {
        db.run {
            menuTable
                .join(menuItemTable).on{ case (menu, itemMenu) => menu.id === itemMenu.menuId }
                .join(itemTable).on{ case ((menu, itemMenu), item) => item.id === itemMenu.itemId}
                .filter{ case ((menu, _), _) => menu.name === name}
                .map{ case ((menu, itemMenu), item) => (menu, item) }
                .result
        }.map(_
            .groupBy(_._1)
            .map{case (menu, seq) => MenuDto(menu.id, menu.name, seq.map(_._2).toSeq)}
            .headOption)
    }

    override def deleteMenu(name: String): Future[Unit] = {
        db.run{
            DBIO.seq (
                menuItemTable.filter(_.menuId in menuTable.filter(_.name === name).map(_.id)).delete,
                menuTable.filter(_.name === name).delete
            ).transactionally
        }
    }
}