package scalaconf.mvn.repo

import akka.actor.ActorSystem
import akka.http.Http
import akka.http.model.HttpMethods._
import akka.http.model._
import akka.io.IO
import akka.pattern.ask
import akka.stream.FlowMaterializer
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scalaconf.mvn.repo.router.RequestRouter

object BasicHttp extends App{

  implicit val system = ActorSystem()
  implicit val materializer = FlowMaterializer()
  implicit val askTimeout: Timeout = 5000.millis

  private val requestRouter = system.actorOf(RequestRouter.props())

  val requestHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(GET, path, _, _, _) => (requestRouter ? path).map(_.asInstanceOf[HttpResponse])
    case _                               => Future(HttpResponse(StatusCodes.BadRequest, entity = "Unknown resource!"))
  }

  val bindingFuture = IO(Http) ? Http.Bind(interface = ConfigFactory.load().getString("host"), port = 9020)
  bindingFuture foreach {
    case Http.ServerBinding(localAddress, connectionStream) =>
      Flow(connectionStream).foreach({
        case Http.IncomingConnection(remoteAddress, requestProducer, responseConsumer) =>
          Flow(requestProducer).mapFuture(requestHandler).produceTo(responseConsumer)
      })
  }

  system.awaitTermination()

}
