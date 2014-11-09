import sbt._

object Version {
  val akka      = "2.3.6"
  val akkaHttp  = "0.9"
  val logback   = "1.1.2"
  val scala     = "2.11.2"
  val scalaTest = "2.2.2"
  val commonIo  = "2.4"
  val asyncHttpClient = "1.8.14"
  val qiniu     = "6.1.7.1"
}

object Library {
  val akkaActor       = "com.typesafe.akka" %% "akka-actor"                    % Version.akka
  val akkaPersistence = "com.typesafe.akka" %% "akka-persistence-experimental" % Version.akka
  val akkaStream      = "com.typesafe.akka" %% "akka-stream-experimental"      % Version.akkaHttp
  val akkaHttp        = "com.typesafe.akka" %% "akka-http-core-experimental"   % Version.akkaHttp
  val akkaSlf4j       = "com.typesafe.akka" %% "akka-slf4j"                    % Version.akka
  val akkaTestkit     = "com.typesafe.akka" %% "akka-testkit"                  % Version.akka
  val logbackClassic  = "ch.qos.logback"    %  "logback-classic"               % Version.logback
  val scalaTest       = "org.scalatest"     %% "scalatest"                     % Version.scalaTest
  val commonIo        = "commons-io"        %  "commons-io"                    % Version.commonIo
  val asyncHttpClient = "com.ning"          %  "async-http-client"             % Version.asyncHttpClient
  val qiniu           = "com.qiniu"         %  "qiniu-java-sdk"                % Version.qiniu
}

object Dependencies {

  import Library._

  val mvnRepo = List(
    akkaActor,
    akkaPersistence,
    akkaStream,
    akkaHttp,
    akkaSlf4j,
    logbackClassic,
    asyncHttpClient,
    commonIo,
    qiniu,
    akkaTestkit % "test",
    scalaTest   % "test"
  )

}
