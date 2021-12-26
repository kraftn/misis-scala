package ru.misis.model

import slick.lifted.Tag
import slick.jdbc.PostgresProfile.api._

case class User(id: Int, name: String, age: Int, countryOfResidence: String)

trait UserRepo {

  class UserTable(tag: Tag) extends Table[User](tag, "User") {
    val id = column[Int]("id", O.PrimaryKey)
    val name = column[String]("name")
    val age = column[Int]("age")
    val countryOfResidence = column[String]("residence_country")
    def * = (
      id,
      name,
      age,
      countryOfResidence
    ) <> ((User.apply _).tupled, User.unapply)
  }

  val userTable = TableQuery[UserTable]
}