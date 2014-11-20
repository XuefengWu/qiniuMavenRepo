package scalaconf.mvn.repo
package store

import java.io.File

import org.iq80.leveldb.impl.Iq80DBFactory._
import org.iq80.leveldb.{DBIterator, Options}


object FetchResult extends Enumeration {
  type Status = Value
  val InProgress, Ok,Fail, NotFound = Value
}

trait FetchStoreDao {

  def put(path: String, status: FetchResult.Status): Unit

  def get(path: String): Option[FetchResult.Status]

  def del(path:String) :Unit

  def fails(): List[String]

  def search(pattern: String): List[String]

}

object FetchStore extends FetchStoreDao {
  private val db = factory.open(new File("fetcher_result_status"), new Options())
  def put(path: String, status: FetchResult.Status): Unit = db.put(bytes(path), bytes(status.toString))
  def get(path: String): Option[FetchResult.Status] = Option(asString(db.get(bytes(path)))).map(FetchResult.withName)
  def del(path:String) :Unit = db.delete(bytes(path))

  def fails(): List[String] = {
    val res = new scala.collection.mutable.ListBuffer[String]
    val iterator: DBIterator = db.iterator()
    while (iterator.hasNext) {
      val ele = iterator.next()
      if (!asString(ele.getValue).isEmpty) {
        Option(asString(ele.getValue)).map(FetchResult.withName) match {
          case Some(FetchResult.Fail) => res += asString(ele.getKey)
          case Some(FetchResult.NotFound) => res += asString(ele.getKey)
          case _ =>
        }
      }
    }
    res.toList
  }

  def search(pattern: String): List[String] = {
    val res = new scala.collection.mutable.ListBuffer[String]
    val iterator: DBIterator = db.iterator()
    while (iterator.hasNext) {
      val ele = iterator.next()
      val artifact: String = asString(ele.getKey)
      if (artifact.startsWith(pattern)) {
        if (!asString(ele.getValue).isEmpty) {
          Option(asString(ele.getValue)).map(FetchResult.withName) match {
            case Some(FetchResult.Ok) => res += artifact
            case _ =>
          }
        }
      }
    }
    res.toList
  }
}