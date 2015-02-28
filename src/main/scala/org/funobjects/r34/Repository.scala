package org.funobjects.r34

import org.scalactic.{Every, Good, Or}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

/**
 * A service or structure which maintains a value type V
 * with a key type of K.
 */
trait Repository[K, V] {

  val syncTimeout = 1.second

  def get(key: K): Future[Option[V] Or Every[Issue]]
  def put(key: K, value: V): Future[Option[V] Or Every[Issue]]
  def remove(key: K): Future[Option[V] Or Every[Issue]]

  def getSync(key: K): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)

  def putSync(key: K, value: V): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)

  def removeSync(key: K): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)
}

class InMemoryRepository[K, V] extends Repository[K, V] {
  val map = collection.concurrent.TrieMap[K, V]()

  override def get(key: K): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.get(key)))

  override def put(key: K, value: V): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.put(key, value)))

  override def remove(key: K): Future[Option[V] Or Every[Issue]] =
    Future.successful(Good(map.remove(key)))
}
