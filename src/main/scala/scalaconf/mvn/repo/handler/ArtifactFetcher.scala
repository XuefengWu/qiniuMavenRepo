package scalaconf.mvn.repo.handler

import java.io.{BufferedInputStream, ByteArrayInputStream, ByteArrayOutputStream}

import akka.actor.{Actor, ActorLogging, Props}
import com.ning.http.client.AsyncHandler.STATE
import com.ning.http.client._
import com.qiniu.api.auth.digest.Mac
import com.qiniu.api.resumableio.ResumeableIoApi
import com.qiniu.api.rs.PutPolicy
import com.typesafe.config.ConfigFactory
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.DefaultHttpClient

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

case class ArtifactUri(resolvers: Seq[String], path: String, clientOpt: Option[AsyncHttpClient] = None)

class ArtifactFetcher(p: PutPolicy, mac: Mac) extends Actor with ActorLogging {


  override def receive: Receive = {
    case ArtifactUri(resolvers, path, _) if resolvers.nonEmpty =>
      if (store.FetchStore.get(path).isEmpty) {
        fetch(resolvers, path, resolvers.head + path)
      }
  }


  private def fetch(resolvers: Seq[String], path: String, url: String): Unit = {
    log.debug(s"fetch $url")
    val httpclient = new DefaultHttpClient()
    val httpgets = new HttpGet(url)
    val response = httpclient.execute(httpgets)

    if (response != null) {
      val status = response.getStatusLine.getStatusCode
      if (status < 400) {
        val relocation = Option(response.getFirstHeader("Location")).map(_.getValue)
        if (relocation.isEmpty) {
          val entity = response.getEntity()

          ResumeableIoApi.put(new BufferedInputStream(entity.getContent, 1024*64), p.token(mac), path.tail)
          store.FetchStore.put(path, store.FetchResult.Ok)
        } else {
          relocation.foreach(ref => fetch(resolvers, path, url))
        }
      } else {
        tryFetchForNext(resolvers, path, url)
      }

    } else {
      tryFetchForNext(resolvers, path, url)
    }


  }

  private def tryFetchForNext(resolvers: Seq[String], path: String, url: String): Unit = {
    if (resolvers.nonEmpty) {
      fetch(resolvers.tail, path, url)
    } else {
      store.FetchStore.put(path, store.FetchResult.Fail)
    }
  }

  private def fetch(resolvers: Seq[String], path: String, url: String, clientOpt: Option[AsyncHttpClient]): Unit = {

    val client = clientOpt.getOrElse(new AsyncHttpClient())
    store.FetchStore.put(path, store.FetchResult.InProgress)
    val f = client.prepareGet(url).execute(new AsyncHandler[Unit]() {

      val bytes = new ByteArrayOutputStream()

      override def onThrowable(t: Throwable): Unit = {

        store.FetchStore.del(path)
        tryFetchForNext(resolvers, path, url, client)
      }

      override def onCompleted(): Unit = {
        val data = bytes.toByteArray
        if (data.size > 0) {
          log.debug(s"=========put data size: ${data.size}=============${path.tail}========")
          ResumeableIoApi.put(new ByteArrayInputStream(data), p.token(mac), path.tail)
          store.FetchStore.put(path, store.FetchResult.Ok)
          client.close()
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
          tryFetchForNext(resolvers, path, url, client)
          STATE.ABORT
        } else {
          STATE.CONTINUE
        }
      }

      override def onHeadersReceived(headers: HttpResponseHeaders): STATE = {
        val location = headers.getHeaders.getFirstValue("Location")
        Option(location).fold(STATE.CONTINUE)(_url => {
          fetch(resolvers, path, _url, clientOpt)
          STATE.ABORT
        })
      }
    })
    if (f.isDone) {
      client.close()
    }

  }

  private def tryFetchForNext(resolvers: Seq[String], path: String, url: String, client: AsyncHttpClient): Unit = {
    if (resolvers.nonEmpty) {
      fetch(resolvers.tail, path, url, Some(client))
      //self ! ArtifactUri(resolvers.tail, path)
    } else {
      store.FetchStore.put(path, store.FetchResult.Fail)
      client.close()
    }
  }

}
