import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl._
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer

import scala.concurrent.Future

object KafkaProducerDemo extends App {
    implicit val system = ActorSystem("akka-streams-demo")
    import system.dispatcher

    val config = system.settings.config.getConfig("akka.kafka.producer")
    val producerSettings =
        ProducerSettings(config, new StringSerializer, new StringSerializer)

    val flow = Flow[Int].map(_.toString)

    val done: Future[Done] =
        Source(1 to 100)
            .via(flow)
            .map(value => new ProducerRecord[String, String]("demo-topic", value))
            .runWith(Producer.plainSink(producerSettings))
}
