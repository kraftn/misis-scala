package ru.misis

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.misis.model.{Item, Menu, User}
import ru.misis.registry.{MenuRegistry, OrderRegistry, UserRegistry}
import ru.misis.registry.MenuRegistry.MenuDto
import ru.misis.registry.OrderRegistry.{OrderDto, OrderItemDto}
import ru.misis.routes.{MenuRoutes, OrderRoutes, UserRoutes}
import ru.misis.services.{InitDB, MenuServiceImpl, OrderServiceImpl, UserServiceImpl}
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

class OrderRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest {
    self =>

    val server: EmbeddedPostgres = EmbeddedPostgres
        .builder()
        .setPort(5334)
        .start()

    lazy val testKit = ActorTestKit()

    implicit def typedSystem = testKit.system
    implicit def default(implicit system: akka.actor.ActorSystem) = RouteTestTimeout(5 seconds)

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

    val userRegistry = new UserRegistry() with UserServiceImpl with init
    val userRegistryActor = testKit.spawn(userRegistry(), "UserRegistryActor")
    val userRoutes = new UserRoutes(userRegistryActor).routes
    // userRegistry.createUser(User(1, "John", 20, "RF"))

    val menuRegistry = new MenuRegistry() with MenuServiceImpl with init
    val menuRegistryActor = testKit.spawn(menuRegistry(), "MenuRegistryActor")
    val menuRoutes = new MenuRoutes(menuRegistryActor).routes
    // menuRegistry.createMenu(MenuDto(1, "daily", Seq(Item(1, "eggs", 100.0), Item(2, "steak", 1000.0))))

    val orderRegistry = new OrderRegistry() with OrderServiceImpl with init
    val orderRegistryActor = testKit.spawn(orderRegistry(), "OrderRegistryActor")
    val orderRoutes = new OrderRoutes(orderRegistryActor).routes

    "OrderRoutes" should {
        "?????????????? ?????????? ??????????????????????????" in {
            val entity1 = Marshal(User(1, "John", 20, "RF")).to[MessageEntity].futureValue
            val request1 = Post(uri = "/users").withEntity(entity1)
            request1 ~> userRoutes

            val entity2 = Marshal(User(2, "Tom", 21, "RF")).to[MessageEntity].futureValue
            val request2 = Post(uri = "/users").withEntity(entity2)
            request2 ~> userRoutes ~> check {
                status should ===(StatusCodes.Created)
            }
        }

        "?????????????? ?????????? ????????" in {
            val menu = MenuDto(1, "daily",
                Seq(
                    Item(1, "eggs", 100.0),
                    Item(2, "steak", 1000.0),
                    Item(3, "tea", 50.0)
                )
            )
            val entity = Marshal(menu).to[MessageEntity].futureValue
            val request = Post(uri = "/menus").withEntity(entity)
            request ~> menuRoutes ~> check {
                status should ===(StatusCodes.Created)
            }
        }

        "???????????????????? ???????????? ???????????? ?????????????? (GET /orders)" in {
            val request = HttpRequest(uri = "/orders")

            request ~> orderRoutes ~> check {
                status should === (StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"orders":[]}""")
            }
        }

        "?????????????????? ?????????? ?????????? (POST /orders)" in {
            val order = OrderDto(1, 1, "Paid",
                Seq(OrderItemDto(Menu(1, "daily"), Item(1, "eggs", 100.0), 100.0, 2),
                    OrderItemDto(Menu(1, "daily"), Item(2, "steak", 1000.0), 1000.0, 1)
                )
            )

            val entity = Marshal(order).to[MessageEntity].futureValue
            val request = Post(uri = "/orders").withEntity(entity)

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.Created)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order ???1 created."}""")
            }
        }

        "???????????????????? ?????????? (GET /order)" in {
            val request = HttpRequest(uri = "/order/1")

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===(
                """{"id":1,"orderItems":[{"item":{"id":1,"name":"eggs","price":100.0},"menu":{"id":1,"name":"daily"},"menuPrice":100.0,"quantity":2},{"item":{"id":2,"name":"steak","price":1000.0},"menu":{"id":1,"name":"daily"},"menuPrice":1000.0,"quantity":1}],"status":"Paid","userId":1}"""
                )
            }
        }

        "?????????????????????? ?????????? (PUT /order)" in {
            val order = OrderDto(1, 1, "Paid",
                Seq(OrderItemDto(Menu(1, "daily"), Item(2, "steak", 1000.0), 1000.0, 1),
                    OrderItemDto(Menu(1, "daily"), Item(3, "tea", 50.0), 50.0, 1)
                )
            )

            val entity = Marshal(order).to[MessageEntity].futureValue
            val request = Put(uri = "/order/1").withEntity(entity)

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order ???1 updated."}""")
            }
        }

        "???????????????? ???????????? ???????????? (PUT /order)" in {
            val request = Put(uri = "/order/1?status=Ready")

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Status has been changed to Ready."}""")
            }
        }

        "?????????????????? ???????????? ?????????? (POST /orders)" in {
            val order = OrderDto(2, 2, "Paid",
                Seq(
                    OrderItemDto(Menu(1, "daily"), Item(3, "tea", 50.0), 50.0, 2)
                )
            )

            val entity = Marshal(order).to[MessageEntity].futureValue
            val request = Post(uri = "/orders").withEntity(entity)

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.Created)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order ???2 created."}""")
            }
        }

        "???????????????????? ???????????? ?????????????? ???? ?????????????? (GET /orders/status)" in {
            val request = HttpRequest(uri = "/orders/Ready")

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===(
                    """{"orders":[{"id":1,"orderItems":[{"item":{"id":2,"name":"steak","price":1000.0},"menu":{"id":1,"name":"daily"},"menuPrice":1000.0,"quantity":1},{"item":{"id":3,"name":"tea","price":50.0},"menu":{"id":1,"name":"daily"},"menuPrice":50.0,"quantity":1}],"status":"Ready","userId":1}]}"""
                )
            }
        }

        "?????????????? ?????????? (DELETE /order)" in {
            val request = Delete(uri = "/order/1")

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"description":"Order ???1 deleted."}""")
            }
        }

        "???????????????????? ???????????? ?????????????? (GET /orders)" in {
            val request = HttpRequest(uri = "/orders")

            request ~> orderRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===(
                    """{"orders":[{"id":2,"orderItems":[{"item":{"id":3,"name":"tea","price":50.0},"menu":{"id":1,"name":"daily"},"menuPrice":50.0,"quantity":2}],"status":"Paid","userId":2}]}"""
                )
            }
        }
    }
}
