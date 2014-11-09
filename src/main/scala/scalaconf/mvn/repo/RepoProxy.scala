package scalaconf.mvn.repo

import akka.actor.{ActorLogging, Actor, ActorRef, Props}
import akka.http.model.{HttpResponse, StatusCodes, Uri, headers}

object RepoProxy {
  def props = Props(new RepoProxy("http://mavenrepo.qiniudn.com"))
}

class RepoProxy(qiniuRoot: String) extends Actor with ActorLogging {

  private val fetchers = scala.collection.mutable.HashMap[String, ActorRef]()

  override def receive: Receive = {
    case uri: Uri =>
      val path = uri.path.toString()
      log.debug(s"request $path")
      if (store.FetchStore.get(path) == Some(store.FetchResult.Ok)) {
        sender() ! HttpResponse(StatusCodes.TemporaryRedirect, headers = List(headers.Location(qiniuRoot + path)))
      } else {
        if (store.FetchStore.get(path).isEmpty) {
          fetch(path)
        }
        sender() ! HttpResponse(StatusCodes.NotFound)
      }

  }

  private def fetch(path: String): Unit = {
    val fetcherActor = fetchers.getOrElseUpdate(path, context.actorOf(ArtifactFetcher.props))
    fetcherActor ! ArtifactUri(Repo.resolvers, path)
  }

}
