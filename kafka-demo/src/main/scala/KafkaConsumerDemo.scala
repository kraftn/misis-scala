import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Committer, Consumer}
import akka.kafka.{CommitterSettings, ConsumerSettings, Subscriptions}
import akka.stream.scaladsl._
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

object KafkaConsumerDemo extends App {
    implicit val system = ActorSystem("akka-streams-demo")

    val config = system.settings.config.getConfig("akka.kafka.consumer")
    val consumerSettings = ConsumerSettings(config, new StringDeserializer, new StringDeserializer)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    val committerSettings = CommitterSettings(system)

    val control =
        Consumer
            .committableSource(consumerSettings, Subscriptions.topics("demo-topic"))
            .map { msg =>
                println(msg)
                msg
            }
            .map(_.committableOffset)
            .toMat(Committer.sink(settings = committerSettings))(Keep.right)
            .run()
}
