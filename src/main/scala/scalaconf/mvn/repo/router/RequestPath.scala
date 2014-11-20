package scalaconf.mvn.repo.router

import akka.actor.ActorRef

case class RequestPath(origin: ActorRef, path: String)
