package org.funobjects.db.orientdb

import akka.actor.Actor.Receive
import com.orientechnologies.orient.core.config.OGlobalConfiguration
import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx

/**
 * Useful common code for dealing with OrientDb
 */
object OrientDbHelper {

  /**
   * Creates a new, empty database, deleting all data in the current database if it currently exists.
   */
  def newDatabase(url: String, user: String, pass: String): ODatabaseDocumentTx = {
    val db = new ODatabaseDocumentTx(url)
    removeDatabase(db, user, pass)
    // create leaves db open, (created with default user/pass)
    db.create()
  }

  /**
   * Deletes the database if it exists,
   */
  def removeDatabase(db: ODatabaseDocumentTx, user: String, pass: String) = {
    if (db.exists()) {
      if (db.isClosed) {
        db.open(user, pass)
      }
      // must be open to drop, and leaves db closed (and non-existent)
      db.drop()
    }
  }

  def openOrCreate(url: String, user: String, pass: String): ODatabaseDocumentTx = {
    val db = new ODatabaseDocumentTx(url)
    try {
      // bad url will cause exception is db.exists(), not in construction
      if (db.exists()) {
        db.open(user, pass)
      } else {
        db.create()
      }
      // if we get here, the db is open, and created if necessary
      db
    } catch {
      // make sure we don't leak connections on error
      case ex: Exception =>
        if (!db.isClosed) {
          db.close()
        }
        throw ex
    }
  }

  def setupThreadContext(db: ODatabaseDocumentTx): Unit = ODatabaseRecordThreadLocal.INSTANCE.set(db)

  def dbState(label: String, db: ODatabaseDocumentTx): String = s"$label.isClosed() = ${db.isClosed}, $label.exists() = ${db.exists()}"
}
