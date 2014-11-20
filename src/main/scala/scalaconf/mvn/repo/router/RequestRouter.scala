package scalaconf.mvn.repo.router

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.model.Uri
import akka.routing.RoundRobinPool

import scalaconf.mvn.repo.handler.{ArtifactSearch, RepoProxy, Troubleshooting}


object RequestRouter {
  private val routes = Map(
    "/fails" -> Troubleshooting.fails,
    "/refresh/" -> Troubleshooting.refresh,
    "/search/" -> ArtifactSearch.props
  )
  def props(): Props = Props(new RequestRouter(routes, RepoProxy.props))
}

class RequestRouter(routers: Map[String, Props], repoProxy: Props) extends Actor with ActorLogging {
  private val actors = scala.collection.mutable.HashMap[String, ActorRef]()
  private val mvnRepoProxy = context.actorOf(RoundRobinPool(100).props(repoProxy))

  override def receive: Receive = {
    case uri: Uri =>
      val path = uri.path.toString()
      log.debug(s"request $path")
      var exists = false
      for (e <- routers) {
        if (path.startsWith(e._1)) {
          val handler = actors.getOrElseUpdate(e._1, context.actorOf(e._2))
          handler ! RequestPath(sender(), extractParam(path, e._1))
          exists = true
        }
      }
      if (!exists) {
        mvnRepoProxy ! RequestPath(sender(), path)
      }
  }

  def extractParam(path: String, route: String): String = path.replace(route, "")

}
