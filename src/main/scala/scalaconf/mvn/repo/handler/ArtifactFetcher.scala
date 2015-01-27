package scalaconf.mvn.repo.handler

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import akka.actor.{Actor, ActorLogging, Props}
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client._
import com.qiniu.api.auth.digest.Mac
import com.qiniu.api.resumableio.ResumeableIoApi
import com.qiniu.api.rs.PutPolicy
import com.typesafe.config.ConfigFactory

import scalaconf.mvn.repo.store

object ArtifactFetcher {
  private val bucketName = "mavenrepo"
  private val p = new PutPolicy(bucketName)
  private val conf = ConfigFactory.load()
  private val ak = conf.getString("ACCESS_KEY")
  private val sk = conf.getString("SECRET_KEY")
  private val mac = new Mac(ak, sk)

  def props() = {
    val mac = new Mac(ak, sk)
    Props(new ArtifactFetcher(p, mac))
  }

}

case class ArtifactUri(resolvers: Seq[String], path: String, clientOpt : Option[AsyncHttpClient] = None)

class ArtifactFetcher(p: PutPolicy, mac: Mac) extends Actor with ActorLogging {


  override def receive: Receive = {
    case ArtifactUri(resolvers, path, clientOpt) if resolvers.nonEmpty =>
      if(store.FetchStore.get(path).isEmpty) {
        fetch(resolvers, path, resolvers.head + path, clientOpt)
      }
  }

  private def fetch(resolvers: Seq[String], path: String, url: String, clientOpt : Option[AsyncHttpClient] = None): Unit = {

    val client = clientOpt.getOrElse(new AsyncHttpClient())
    store.FetchStore.put(path, store.FetchResult.InProgress)
    val f = client.prepareGet(url).execute(new AsyncHandler[Unit]() {

      val bytes = new ByteArrayOutputStream()

      override def onThrowable(t: Throwable): Unit = {

        store.FetchStore.del(path)
        tryFetchForNext(resolvers, path, client)
      }

      override def onCompleted(): Unit = {
        val data = bytes.toByteArray
        if(data.size > 0){
          log.debug(s"=========put data size: ${data.size}=============${path.tail}========")
          ResumeableIoApi.put(new ByteArrayInputStream(data), p.token(mac), path.tail)
          store.FetchStore.put(path, store.FetchResult.Ok)
        }
      }

      override def onBodyPartReceived(bodyPart: HttpResponseBodyPart): STATE = {
        bytes.write(bodyPart.getBodyPartBytes())
        STATE.CONTINUE
      }

      override def onStatusReceived(responseStatus: HttpResponseStatus): STATE = {
        val statusCode = responseStatus.getStatusCode()
        if (statusCode >= 400) {
          store.FetchStore.del(path)
          tryFetchForNext(resolvers, path, client)
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
    if(f.isDone){
      client.close()
    }

  }

  private def tryFetchForNext(resolvers: Seq[String], path: String, client: AsyncHttpClient): Unit = {
    if(resolvers.nonEmpty) {
      self ! ArtifactUri(resolvers.tail, path)
    } else {
      store.FetchStore.put(path, store.FetchResult.Fail)
      client.close()
    }
  }

}
