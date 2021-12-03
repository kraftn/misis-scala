package ru.misis.model

import scala.collection.immutable

case class User(name: String, age: Int, countryOfResidence: String)

final case class Users(users: immutable.Seq[User])