package scalaconf.mvn.repo

import java.io.File

import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory.factory
import org.iq80.leveldb.impl.Iq80DBFactory.bytes
import org.iq80.leveldb.impl.Iq80DBFactory.asString

object Repo {

  object FetchResult extends Enumeration {
    type Status = Value
    val InProgress, Ok,Fail = Value
  }

  object store {
    val db = factory.open(new File("fetcher_result_status"), new Options())
    def put(path: String, status: FetchResult.Status) = db.put(bytes(path), bytes(status.toString))
    def get(path: String): Option[FetchResult.Status] = Option(asString(db.get(bytes(path)))).map(FetchResult.withName)
  }

  val resolvers = Seq(
    "http://repo.typesafe.com/typesafe/releases/",
    "http://repo.typesafe.com/typesafe/snapshots/"
  )
  
}
