package scalaconf.mvn.repo.router

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.model.{HttpResponse, StatusCodes, Uri}

import scalaconf.mvn.repo.handler.{RepoProxy, Troubleshooting}

/**
 * Component:
 * Description:
 * Date: 2014/11/19
 * @author Andy Ai
 */
object RequestRouter {
  def props(): Props = {
    Props(new RequestRouter(Map(
      "/releases/" -> RepoProxy.props,
      "/snapshot/" -> RepoProxy.props,
      "/fails/" -> Troubleshooting.fails(),
      "/refresh/" -> Troubleshooting.refresh()
    )))
  }
}

class RequestRouter(routers: Map[String, Props]) extends Actor with ActorLogging {
  private val actors = scala.collection.mutable.HashMap[String, ActorRef]()

  override def receive: Receive = {
    case uri: Uri =>
      val path = uri.path.toString()
      log.debug(s"request $path")
      var exists = false
      for (e <- routers) {
        if (path.startsWith(e._1)) {
          val handler = actors.getOrElseUpdate(e._1, context.actorOf(e._2))
          handler ! RequestPath(sender(), extractPath(path, e._1))
          exists = true
        }
      }
      if (!exists) {
        sender() ! HttpResponse(status = StatusCodes.NotFound, entity = s"Cant route: $path")
      }
  }

  def extractPath(path: String, route: String): String = {
    path.replace(route, "")
  }
}
