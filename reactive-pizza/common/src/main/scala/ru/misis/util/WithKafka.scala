package ru.misis.util

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{StringDeserializer, StringSerializer}
import ru.misis.event.Event
import spray.json._

import scala.concurrent.Future
import scala.reflect.ClassTag

trait WithKafka {
    implicit def system: ActorSystem

    val config = system.settings.config

    val producerSettings =
        ProducerSettings(config.getConfig("akka.kafka.producer"), new StringSerializer, new StringSerializer)

    val consumerSettings = ConsumerSettings(config.getConfig("akka.kafka.consumer"), new StringDeserializer, new StringDeserializer)
        .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    def kafkaSource[T <: Event : JsonReader](implicit tag: ClassTag[T]): Source[T, _] = {
        Consumer
            .committableSource(consumerSettings, Subscriptions.topics(tag.runtimeClass.getSimpleName))
            .map(_.record.value().parseJson.convertTo[T])
    }

    def kafkaSink[T <: Event](implicit writer: JsonWriter[T], tag: ClassTag[T]): Sink[T, Future[Done]] = {
        Flow[T]
            .map(value => new ProducerRecord[String, String](tag.runtimeClass.getSimpleName, value.toJson.compactPrint))
            .toMat(Producer.plainSink(producerSettings))(Keep.right)
    }

    def publishEvent[T <: Event](event: T)(implicit writer: JsonWriter[T], tag: ClassTag[T]): Future[Done] = {
        Source.single(event).runWith(kafkaSink)
    }
}
