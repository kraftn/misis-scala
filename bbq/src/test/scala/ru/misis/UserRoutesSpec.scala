package ru.misis

//#user-routes-spec
//#test-top

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.misis.model.User
import ru.misis.registry.UserRegistry
import ru.misis.routes.UserRoutes
import ru.misis.services.{InitDB, UserServiceImpl}
import slick.jdbc.PostgresProfile.api._

//#set-up
class UserRoutesSpec extends AnyWordSpec with Matchers with ScalaFutures with ScalatestRouteTest { self =>
    //#test-top

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

    val userRegistry = new UserRegistry() with UserServiceImpl with init
    val userRegistryActor = testKit.spawn(userRegistry(), "UserRegistryActor")
    val userRoutes = new UserRoutes(userRegistryActor).routes

    //#set-up

    //#actual-test
    "UserRoutes" should {
        "return no users if no present (GET /users)" in {
            // note that there's no need for the host part in the uri:
            val request = HttpRequest(uri = "/users")

            request ~> userRoutes ~> check {
                status should ===(StatusCodes.OK)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and no entries should be in the list:
                entityAs[String] should ===("""{"users":[]}""")
            }
        }
        //#actual-test

        //#testing-post
        "be able to add users (POST /users)" in {
            val user = User(1, "Kapi", 42, "jp")
            val userEntity = Marshal(user).to[MessageEntity].futureValue // futureValue is from ScalaFutures

            // using the RequestBuilding DSL:
            val request = Post("/users").withEntity(userEntity)

            request ~> userRoutes ~> check {
                status should ===(StatusCodes.Created)

                // we expect the response to be json:
                contentType should ===(ContentTypes.`application/json`)

                // and we know what message we're expecting back:
                entityAs[String] should ===("""{"description":"User Kapi created."}""")
            }
        }
        //#testing-post

        "update user (PUT /user)" in {
            val userName: String = "Kapi"
            val newUser: User = User(1, "John", 42, "RF")
            val userEntity = Marshal(newUser).to[MessageEntity].futureValue

            val request = Put(s"/user/$userName").withEntity(userEntity)
            request ~> userRoutes ~> check {
                status should === (StatusCodes.OK)
                contentType should === (ContentTypes.`application/json`)
                entityAs[String] should === ("""{"description":"User Kapi updated."}""")
            }
        }

        "get user (GET /user)" in {
            val request = HttpRequest(uri = "/user/John")

            request ~> userRoutes ~> check {
                status should === (StatusCodes.OK)
                contentType should === (ContentTypes.`application/json`)
                entityAs[String] should === ("""{"age":42,"countryOfResidence":"RF","id":1,"name":"John"}""")
            }
        }

        "be able to remove users (DELETE /users)" in {
          // user the RequestBuilding DSL provided by ScalatestRouteSpec:
          val request = Delete(uri = "/user/John")

          request ~> userRoutes ~> check {
            status should ===(StatusCodes.OK)

            // we expect the response to be json:
            contentType should ===(ContentTypes.`application/json`)

            // and no entries should be in the list:
            entityAs[String] should ===("""{"description":"User John deleted."}""")
          }
        }
        //#actual-test
    }
    //#actual-test

    //#set-up
}
//#set-up
//#user-routes-spec
