package ru.misis.model

case class Item(name: String, price: Double)

case class Items(items: Seq[Item])