package org.funobjects.r34

import org.scalactic.{Bad, Every, Good, Or}

import scala.concurrent.{ExecutionContext, Await, Future}
import scala.concurrent.duration.DurationInt

/**
 * A read-only or structure which maintains a value type V
 * with a key type of K.
 */
trait Repository[K, V] { self =>

  val syncTimeout = 1.second

  def get(key: K): Future[Option[V] Or Every[Issue]]

  def getSync(key: K): Option[V] Or Every[Issue] =
    Await.result(get(key), syncTimeout)

  def orElse(nextRepo: Repository[K, V])(implicit exec: ExecutionContext): Repository[K, V] = new Repository[K, V] {

    override def get(key: K): Future[Or[Option[V], Every[Issue]]] = {
      self.get(key) flatMap {
        case Good(None) => nextRepo.get(key)
        case g @ Good(_) => Future.successful(g)
        case b @ Bad(_) => Future.successful(b)
      }
    }
  }
}


