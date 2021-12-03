package ru.misis.model

case class Menu(name: String, items: Seq[String])

case class Menus(menus: Seq[Menu])
