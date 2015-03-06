package org.funobjects.r34

import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by rgf on 3/3/15.
 */
class QueueFlow[IN, OUT] {

  val q: java.util.concurrent.BlockingQueue[IN] = new LinkedBlockingQueue[IN]()

  //def flow()
}