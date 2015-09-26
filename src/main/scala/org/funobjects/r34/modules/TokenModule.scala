package org.funobjects.r34.modules

import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import org.funobjects.r34.Deletable
import org.funobjects.r34.auth.{Permits, BearerToken}
import org.funobjects.r34.modules.TokenModule.TokenEntry

import scala.concurrent.ExecutionContext

/**
 * Created by rgf on 6/10/15.
 */
class TokenModule[U](implicit sys: ActorSystem, exec: ExecutionContext, flows: FlowMaterializer) extends StorageModule[TokenEntry[U]]("token")(sys, exec, flows) {

  implicit def entryDeletable = new Deletable[TokenEntry[_]] {
    override def isDeleted(entry: TokenEntry[_]): Boolean = ???

    override def delete(entry: TokenEntry[_]): TokenEntry[_] = ???

    override def deletedTime(entry: TokenEntry[_]): Long = ???
  }


//  /**
//   * Determines if an entity has been deleted.
//   */
//  override def isDeleted(entry: TokenEntry[U]): Boolean = entry.deleted
//
//  /**
//   * Returns a copy of the entity which has been marked for deletion.
//   */
//  override def delete(entity: TokenEntry): TokenEntry[U] = entity.copy(deleted = true)
}

object TokenModule {
  case class TokenEntry[U](user: U, papers: Permits, expires: Option[Long], deleted: Boolean = false)
}
