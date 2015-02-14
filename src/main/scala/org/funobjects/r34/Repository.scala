package org.funobjects.r34

import org.scalactic.{Every, Good, Or}

import scala.concurrent.Future

/**
 * A service or structure which maintains a value type V
 * with a key type of K.
 */
trait Repository[K, V] {
  def get(key: K): Future[Option[V] Or Every[Issue]]
  def put(key: K, value: V): Future[Option[V] Or Every[Issue]]
  def remove(key: K): Future[Option[V] Or Every[Issue]]
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
