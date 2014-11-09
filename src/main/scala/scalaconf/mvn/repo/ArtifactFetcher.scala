package scalaconf.mvn.repo

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import akka.actor.{Actor, Props}
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client._
import com.qiniu.api.auth.digest.Mac
import com.qiniu.api.resumableio.ResumeableIoApi
import com.qiniu.api.rs.PutPolicy

object ArtifactFetcher {

  private val bucketName = "mavenrepo"
  private val p = new PutPolicy(bucketName)
  private val ACCESS_KEY = "*****"
  private val SECRET_KEY = "*****"
  private val mac = new Mac(ACCESS_KEY, SECRET_KEY)

  def props() = Props(new ArtifactFetcher(p, mac))
}

case class ArtifactUri(resolvers: Seq[String], path: String)

class ArtifactFetcher(p: PutPolicy, mac: Mac) extends Actor {


  override def receive: Receive = {
    case ArtifactUri(resolvers, path) if resolvers.nonEmpty =>
      if(Repo.store.get(path).isEmpty) {
        fetch(resolvers, path, resolvers.head + path)
      }
  }

  private def fetch(resolvers: Seq[String], path: String, url: String): Unit = {

    val client = new AsyncHttpClient()
    Repo.store.put(path, Repo.FetchResult.InProgress)
    client.prepareGet(url).execute(new AsyncHandler[Unit]() {

      val bytes = new ByteArrayOutputStream()

      override def onThrowable(t: Throwable): Unit = {
        Repo.store.put(path, Repo.FetchResult.Fail)
        tryFetchForNext(resolvers, path)
      }

      override def onCompleted(): Unit = {
        ResumeableIoApi.put(new ByteArrayInputStream(bytes.toByteArray), p.token(mac), path.tail)
        Repo.store.put(path, Repo.FetchResult.Ok)
      }

      override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
        bytes.write(bodyPart.getBodyPartBytes())
        STATE.CONTINUE
      }

      override def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
        val statusCode = responseStatus.getStatusCode()
        if (statusCode >= 400) {
          Repo.store.put(path, Repo.FetchResult.Fail)
          tryFetchForNext(resolvers, path)
          STATE.ABORT
        } else {
          STATE.CONTINUE
        }
      }

      override def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
        val location = headers.getHeaders.getFirstValue("Location")
        Option(location).fold(STATE.CONTINUE)(_url => {
          fetch(resolvers, path, _url)
          STATE.ABORT
        })
      }
    })

  }

  private def tryFetchForNext(resolvers: Seq[String], path: String): Unit = {
    if(resolvers.nonEmpty) {
      self ! ArtifactUri(resolvers.tail, path)
    }
  }

}
