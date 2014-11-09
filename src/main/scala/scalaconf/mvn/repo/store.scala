package scalaconf.mvn.repo
package store

import java.io.File

import org.iq80.leveldb.Options
import org.iq80.leveldb.impl.Iq80DBFactory._


object FetchResult extends Enumeration {
  type Status = Value
  val InProgress, Ok,Fail = Value
}

object FetchStore {
  private val db = factory.open(new File("fetcher_result_status"), new Options())
  def put(path: String, status: FetchResult.Status) = db.put(bytes(path), bytes(status.toString))
  def get(path: String): Option[FetchResult.Status] = Option(asString(db.get(bytes(path)))).map(FetchResult.withName)
}