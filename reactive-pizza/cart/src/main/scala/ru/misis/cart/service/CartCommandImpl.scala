package ru.misis.cart.service

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse}
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.delete.{DeleteByQueryResponse, DeleteResponse}
import com.sksamuel.elastic4s.requests.get.GetResponse
import com.sksamuel.elastic4s.requests.indexes.IndexResponse
import com.sksamuel.elastic4s.requests.searches.SearchResponse
import com.sksamuel.elastic4s.requests.task.CreateTaskResponse
import com.sksamuel.elastic4s.requests.update.UpdateResponse
import com.sksamuel.elastic4s.{ElasticClient, RequestSuccess, Response}
import ru.misis.event.Menu.Menu
import ru.misis.event.Cart.{CartId, Order, OrderItem}
import ru.misis.event.Payment.Cheque
import ru.misis.event.EventJsonFormats._
import ru.misis.cart.model.CartCommands
import ru.misis.cart.model.ModelJsonFormats._
import ru.misis.cart.model.Objects._
import ru.misis.util.WithKafka
import spray.json._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import ru.misis.event.OrderStatuses.OrderStatus
import spray.json.DefaultJsonProtocol._

import scala.concurrent.{ExecutionContext, Future}

class CartCommandImpl(elastic: ElasticClient)(implicit executionContext: ExecutionContext,
                                              val system: ActorSystem)
    extends CartCommands
    with WithKafka {

    val menuIndex = "menu"
    val cartIndex = "cart"

    elastic.execute {
        deleteIndex(menuIndex)
    }
        .flatMap { _ =>
            elastic.execute {
                createIndex(menuIndex).mapping(
                    properties(
                        nestedField("categories").properties(
                            textField("name"),
                            nestedField("items").properties(
                                keywordField("id"),
                                textField("name"),
                                textField("description"),
                                doubleField("price")
                            )
                        )
                    )
                )
            }
        }

    elastic.execute {
        deleteIndex(cartIndex)
    }
        .flatMap { _ =>
            elastic.execute {
                createIndex(cartIndex).mapping(
                    properties(
                        keywordField("id"),
                        nestedField("items").properties(
                            keywordField("menuItemId"),
                            intField("amount")
                        )
                    )
                )
            }
        }

    override def replaceMenu(menu: Menu): Future[Menu] =
        elastic.execute {
            deleteByQuery(menuIndex, matchAllQuery())
        }.flatMap {
            case _: RequestSuccess[Either[DeleteByQueryResponse, CreateTaskResponse]] =>
                elastic.execute {
                    indexInto(menuIndex).doc(menu)
                }.map {
                    case _: RequestSuccess[IndexResponse] => menu
                }
        }

    override def getMenu: Future[Menu] =
        elastic.execute {
            search(menuIndex).size(1)
        }.map {
            case results: RequestSuccess[SearchResponse] => results.result.to[Menu].head
        }

    override def updateCart(cartId: CartId, cartItem: CartItem): Future[Cart] =
        elastic.execute {
            get(cartIndex, cartId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val cart = {
                    if (results.result.found) results.result.to[Cart]
                    else Cart(cartId, Nil)
                }
                val updatedCart = {
                    if (cartItem.amount == 0)
                        cart.copy(items = cart.items.filterNot(_.menuItemId == cartItem.menuItemId))
                    else
                        cart.copy(items = cart.items.filterNot(_.menuItemId == cartItem.menuItemId) :+ cartItem)
                }
                elastic.execute {
                    updateById(cartIndex, cartId).docAsUpsert(updatedCart)
                }.map {
                    case _: RequestSuccess[UpdateResponse] => updatedCart
                }
        }

    override def deleteCart(cartId: CartId): Future[Response[DeleteResponse]] =
        elastic.execute {
            deleteById(cartIndex, cartId)
        }

    override def payForOrder(order: Order): Future[HttpResponse] = {
        val request = HttpRequest(
            method = HttpMethods.POST,
            uri = s"http://${sys.env("PAYMENT_SERVER")}/pay",
            entity = HttpEntity(ContentTypes.`application/json`, order.toJson.compactPrint)
        )
        Http().singleRequest(request)
    }

    override def createOrder(cartId: CartId, status: OrderStatus): Future[Order] =
        elastic.execute {
            get(cartIndex, cartId)
        }.flatMap {
            case results: RequestSuccess[GetResponse] =>
                val cart = results.result.to[Cart]
                getMenu.map { menu =>
                    val orderItems = for {
                        cartItem <- cart.items
                        category <- menu.categories
                        menuItem <- category.items
                        if menuItem.id == cartItem.menuItemId
                    } yield OrderItem(
                        menuItemId = menuItem.id,
                        name = menuItem.name,
                        price = menuItem.price,
                        amount = cartItem.amount,
                        cookedAmount = 0
                    )
                    Order(cartId, orderItems, status)
                }
        }
}
