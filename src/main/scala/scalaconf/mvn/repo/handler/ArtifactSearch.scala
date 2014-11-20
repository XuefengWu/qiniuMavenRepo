package scalaconf.mvn.repo.handler

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.model.{HttpResponse, StatusCodes}

import scalaconf.mvn.repo.router.RequestPath
import scalaconf.mvn.repo.store.FetchStore

object ArtifactSearch {
  def props(): Props = {
    Props(new ArtifactSearch())
  }
}

class ArtifactSearch extends Actor with ActorLogging {
  override def receive: Receive = {
    case RequestPath(origin, path) =>
      log.debug(s"search $path")
      FetchStore.search(path) match {
        case Nil =>
          origin ! HttpResponse(status = StatusCodes.NotFound, entity = s"Not artifacts matched, path: $path")
        case artifacts =>
          origin ! HttpResponse(status = StatusCodes.OK, entity = FetchStore.search(path).mkString("\n"))
      }
  }
}
