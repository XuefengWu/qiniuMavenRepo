package scalaconf.mvn.repo.handler

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.http.model.{HttpResponse, StatusCodes, headers}

import scalaconf.mvn.repo.router.RequestPath
import scalaconf.mvn.repo.{Repo, store}

object RepoProxy {
  def props = Props(new RepoProxy("http://mavenrepo.qiniudn.com"))
}

class RepoProxy(qiniuRoot: String) extends Actor with ActorLogging {

  private val fetchers = scala.collection.mutable.HashMap[String, ActorRef]()

  override def receive: Receive = {
    case RequestPath(origin, path) =>
      log.debug(s"proxy $path")
      if (store.FetchStore.get(path) == Some(store.FetchResult.Ok)) {
        origin ! HttpResponse(StatusCodes.TemporaryRedirect, headers = List(headers.Location(qiniuRoot + path)))
      } else {
        if (store.FetchStore.get(path).isEmpty) {
          fetch(path)
        }
        origin ! HttpResponse(StatusCodes.NotFound, entity = s"artifact cant found: $path")
      }

  }

  private def fetch(path: String): Unit = {
    val fetcherActor = fetchers.getOrElseUpdate(path, context.actorOf(ArtifactFetcher.props))
    fetcherActor ! ArtifactUri(Repo.resolvers, path)
  }

}
