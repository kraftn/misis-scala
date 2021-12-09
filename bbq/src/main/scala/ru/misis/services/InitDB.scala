package ru.misis.services

import ru.misis.model.ItemRepo
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Future

class InitDB(db: Database) extends ItemRepo {

    def prepareRepository(): Future[Unit] = {
        db.run { DBIO.seq(
            itemTable.schema.createIfNotExists
        )}
    }
}
