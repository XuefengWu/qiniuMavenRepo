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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object BasicHttp extends App{

  implicit val system = ActorSystem()
  implicit val materializer = FlowMaterializer()
  implicit val askTimeout: Timeout = 500.millis

  private val fecherActor = system.actorOf(RepoProxy.props)

  val requestHandler: HttpRequest ⇒ Future[HttpResponse] = {
    case HttpRequest(GET, path, _, _, _) ⇒ fetchArtifact(path)
    case _                               ⇒ Future(HttpResponse(StatusCodes.BadRequest, entity = "Unknown resource!"))
  }

  def fetchArtifact(uri: Uri): Future[HttpResponse] = (fecherActor ? uri).map(_.asInstanceOf[HttpResponse])

  val bindingFuture = IO(Http) ? Http.Bind(interface = "localhost", port = 9020)
  bindingFuture foreach {
    case Http.ServerBinding(localAddress, connectionStream) ⇒
      Flow(connectionStream).foreach({
        case Http.IncomingConnection(remoteAddress, requestProducer, responseConsumer) ⇒
          Flow(requestProducer).mapFuture(requestHandler).produceTo(responseConsumer)
      })
  }

  system.awaitTermination()

}
