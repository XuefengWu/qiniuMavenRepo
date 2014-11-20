package scalaconf.mvn.repo.handler

import akka.actor.{Actor, ActorLogging, Props}
import akka.http.model.{HttpResponse, StatusCodes}

import scalaconf.mvn.repo.router.RequestPath
import scalaconf.mvn.repo.store.FetchStore

object Troubleshooting {
  def refresh(): Props = {
    Props(classOf[Refresh])
  }

  def fails(): Props = {
    Props(classOf[Fails])
  }
}

class Refresh extends Actor with ActorLogging {
  override def receive: Receive = {
    case RequestPath(origin, path) =>
      FetchStore.del(path)
      origin ! HttpResponse(status = StatusCodes.OK, entity = "Success. you can try download at later.")
  }
}

class Fails extends Actor with ActorLogging {
  override def receive: Actor.Receive = {
    case RequestPath(origin, _) =>
      origin ! HttpResponse(status = StatusCodes.OK, entity = FetchStore.fails().mkString("\n"))
  }
}
