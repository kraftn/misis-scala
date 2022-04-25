import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.Future

object StreamsDemo extends App {
    implicit val system = ActorSystem("akka-streams-demo")
    import system.dispatcher

    val source: Source[Int, NotUsed] = Source(1 to 1000)
    val flow = Flow[Int].map(_ * 2)
    val sink: Sink[Any, Future[Done]] = Sink.foreach(println)

    val graph = source.via(flow).to(sink)
//    graph.run()

    val sinkFold = Sink.fold[Int, Int](0)(_ + _)

    val graphFold = source.via(flow).toMat(sinkFold)(Keep.right)


    source.via(flow).runWith(sinkFold)
        .map(i => println(s"result ${i}"))

}
