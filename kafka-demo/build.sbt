lazy val akkaVersion = "2.6.19"

lazy val root = (project in file(".")).
    settings(
        inThisBuild(List(
            organization := "ru.misis",
            scalaVersion := "2.13.4"
        )),
        name := "kafka-demo",
        libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-stream" % akkaVersion,
            "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
            "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
            "ch.qos.logback" % "logback-classic" % "1.2.3"
        )
    )
