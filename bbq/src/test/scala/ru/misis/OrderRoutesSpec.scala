package ru.misis

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.misis.model.{Item, Menu, User}
import ru.misis.registry.{MenuRegistry, OrderRegistry, UserRegistry}
import ru.misis.registry.MenuRegistry.MenuDto
import ru.misis.registry.OrderRegistry.OrderDto
import ru.misis.routes.OrderRoutes
import ru.misis.services.{InitDB, MenuServiceImpl, OrderServiceImpl, UserServiceImpl}
import slick.jdbc.PostgresProfile.api._

class OrderRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest { self =>

    val server: EmbeddedPostgres = EmbeddedPostgres
        .builder()
        .setPort(5334)
        .start()


    lazy val testKit = ActorTestKit()
    implicit def typedSystem = testKit.system
    val db: Database = Database.forURL(url = server.getJdbcUrl("postgres", "postgres"),
        driver = "org.postgresql.Driver")
    trait init {
        def db = self.db
        implicit val executionContext = self.typedSystem.executionContext
    }

    override def createActorSystem(): akka.actor.ActorSystem =
        testKit.system.classicSystem

    import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
    import JsonFormats._

    val repo = new InitDB(db)

    repo.cleanRepository().flatMap(_ => repo.prepareRepository())

    val orderRegistry = new OrderRegistry() with OrderServiceImpl with init
    val orderRegistryActor = testKit.spawn(orderRegistry(), "OrderRegistryActor")
    val orderRoutes = new OrderRoutes(orderRegistryActor).routes

    "OrderRoutes" should {
        "возвращает пустой список заказов (GET /orders)" in {
            val userRegistry = new UserRegistry() with UserServiceImpl with init
            userRegistry.createUser(User(1, "John", 20, "RF"))

            val menuRegistry = new MenuRegistry() with MenuServiceImpl with init
            menuRegistry.createMenu(MenuDto(1, "daily", Seq(Item(1, "eggs", 100.0), Item(2, "steak", 1000.0))))

            val request = HttpRequest(uri = "/orders")

            request ~> orderRoutes ~> check {
                // status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"orders":[]}""")
            }
        }

        "добавляет новый заказ (POST /orders)" in {
            val order = OrderDto(1, Item(1, "eggs", 100.0), Menu(1, "daily"), 100.0,
                User(1, "John", 20, "RF"), 2, "Paid")

            val entity = Marshal(order).to[MessageEntity].futureValue
            val request = Post(uri = "/orders").withEntity(entity)

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.Created)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order 1 created."}""")
            }
        }

        "возвращает заказ (GET /order)" in {
            val request = HttpRequest(uri = "/order/1")

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"item":{"id":1,"name":"eggs","price":100.0},"menu":{"id":1,"name":"daily"},"menuPrice":100.0,"orderId":1,"quantity":2,"status":"Paid","user":{"age":20,"countryOfResidence":"RF","id":1,"name":"John"}}""")
            }
        }

        "редактирует заказ (PUT /order)" in {
            val order = OrderDto(1, Item(2, "steak", 1000.0), Menu(1, "daily"), 1000.0,
                User(1, "John", 20, "RF"), 2, "Paid")

            val entity = Marshal(order).to[MessageEntity].futureValue
            val request = Put(uri = "/order/1").withEntity(entity)

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order 1 updated."}""")
            }
        }

        "изменяет статус заказа (PUT /order)" in {
            val request = Put(uri = "/order/1?status=Ready")

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Status has been changed to Ready."}""")
            }
        }

        "добавляет второй заказ (POST /orders)" in {
            val order = OrderDto(2, Item(1, "eggs", 100.0), Menu(1, "daily"), 100.0,
                User(1, "John", 20, "RF"), 5, "Paid")

            val entity = Marshal(order).to[MessageEntity].futureValue
            val request = Post(uri = "/orders").withEntity(entity)

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.Created)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order 2 created."}""")
            }
        }

        "возвращает список заказов по статусу (GET /orders/status)" in {
            val request = HttpRequest(uri = "/orders/Ready")

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"orders":[{"item":{"id":2,"name":"steak","price":1000.0},"menu":{"id":1,"name":"daily"},"menuPrice":1000.0,"orderId":1,"quantity":2,"status":"Ready","user":{"age":20,"countryOfResidence":"RF","id":1,"name":"John"}}]}""")
            }
        }

        "удаляет заказ (DELETE /order)" in {
            val request = Delete(uri = "/order/1")

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order 1 deleted."}""")
            }
        }
    }
}
