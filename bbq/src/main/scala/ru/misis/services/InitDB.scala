package ru.misis.services

import ru.misis.model.{ItemRepo, MenuRepo}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class InitDB(db: Database) extends ItemRepo with MenuRepo{

    def prepareRepository(): Future[Unit] = {
        db.run { DBIO.seq(
            itemTable.schema.createIfNotExists,
            menuItemTable.schema.createIfNotExists,
            menuTable.schema.createIfNotExists
        )}
    }


    def cleanRepository(): Future[Unit] = {
        db.run { DBIO.seq(
            itemTable.schema.dropIfExists,
            menuItemTable.schema.dropIfExists,
            menuTable.schema.dropIfExists
        )}
    }

}
