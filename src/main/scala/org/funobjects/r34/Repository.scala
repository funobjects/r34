package org.funobjects.r34

import org.scalactic.{Bad, Every, Good, Or}

import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration.DurationInt

/**
 * A service or structure which maintains a value type V
 * with a key type of K.
 */
trait Repository[K, V] { self =>

  implicit val exec: ExecutionContext

  val syncTimeout = 1.second

  def getEntry(key: K): Future[Option[V] Or Every[Issue]]
  def putEntry(key: K, value: V): Future[Option[V] Or Every[Issue]]
  def removeEntry(key: K): Future[Option[V] Or Every[Issue]]

  def get(key: K): Future[Option[V] Or Every[Issue]] = getEntry(key)
  def put(key: K, value: V): Future[Option[V] Or Every[Issue]] = putEntry(key, value)
  def remove(key: K): Future[Option[V] Or Every[Issue]] = removeEntry(key)

  def getSync(key: K): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)

  def putSync(key: K, value: V): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)

  def removeSync(key: K): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)

  def orElse(nextRepo: Repository[K, V]): Repository[K, V] = new Repository[K, V] {

    override implicit val exec: ExecutionContext = implicitly[ExecutionContext]

    override def getEntry(key: K): Future[Or[Option[V], Every[Issue]]] =
      self.getEntry(key) flatMap {
        case g @ Good(Some(v)) => Future.successful(Good(Some(v)))
        case b @ Bad(_) => Future.successful(b)
        case Good(None) => nextRepo.get(key)
      }

    override def putEntry(key: K, value: V): Future[Or[Option[V], Every[Issue]]] =
      self.putEntry(key, value)

    override def removeEntry(key: K): Future[Or[Option[V], Every[Issue]]] =
      self.removeEntry(key)
  }
}

class InMemoryRepository[K, V](implicit val exec: ExecutionContext) extends Repository[K, V] {
  val map = collection.concurrent.TrieMap[K, V]()

  override def getEntry(key: K): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.get(key)))

  override def putEntry(key: K, value: V): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.put(key, value)))

  override def removeEntry(key: K): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.remove(key)))
}
