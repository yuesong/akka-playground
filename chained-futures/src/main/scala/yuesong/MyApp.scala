package yuesong

import akka.actor._
import akka.dispatch._
import org.slf4j.LoggerFactory

/**
 * When chaining Futures that use different dispatchers, it is important to understand that all callback methods such as
 * onResult, onException, even flatMap happen on a thread in the outer future's dispatcher, therefore they are subject
 * to queuing like other tasks on the dispatch. For example, if you simultaneously created more outer futures on the 
 * dispatcher than the number of thread it has, the inner futures's code won't be executed until all outer futures
 * complete! 
 */
object MyApp extends App {

  final val log = LoggerFactory.getLogger(getClass)

  final val DispatcherA = Dispatchers.newExecutorBasedEventDrivenDispatcher("disp-A")
    .withNewThreadPoolWithLinkedBlockingQueueWithUnboundedCapacity
    .setCorePoolSize(2)
    .setMaxPoolSize(2)
    .build

  final val DispatcherB = Dispatchers.newExecutorBasedEventDrivenDispatcher("disp-B")
    .withNewThreadPoolWithLinkedBlockingQueueWithUnboundedCapacity
    .setCorePoolSize(10)
    .setMaxPoolSize(10)
    .build

  def doA(count: Int, value: String, err: Boolean = false): Seq[String] = {
    log.info("doA called: (%s, %s)".format(count, value))
    Thread.sleep(10000)
    if (err) {
      log.info("doA throwing exception")
      sys.error("doA error")
    }
    val result = (1 to count).map(value + _)
    log.info("doA returning: %s".format(result))
    result
  }

  def doB(value: String): String = {
    log.info("doB called: %s".format(value))
    Thread.sleep(10000)
    val result = value.reverse
    log.info("doB returning: %s".format(result))
    result
  }

  def goodRun(): Unit = run(
    List((2, "boo"), (3, "foo"), (4, "bar"), (5, "baz")),
    (count, value) => Future(doA(count, value), 120000)(DispatcherA) flatMap { seq =>
      log.info("handing off (%s, %s)".format(count, value))
      val f = Future.traverse(seq, 120000) { v => Future(doB(v), 120000)(DispatcherB) }
      log.info("inner future created: " + f)
      f
    }
  )
  
  def errorRun(): Unit = run(
    List((2, "boo"), (3, "foo"), (4, "bar"), (5, "baz")),
    // (3, "foo") causes exception
    (count, value) => Future(doA(count, value, count == 3), 120000)(DispatcherA) flatMap { seq =>
      log.info("handing off (%s, %s)".format(count, value))
      val f = Future.traverse(seq, 120000) { v => Future(doB(v), 120000)(DispatcherB) }
      log.info("inner future created: " + f)
      f
    }
  )
  
  def timeoutRun(): Unit = run(
    List((2, "boo"), (3, "foo"), (4, "bar"), (5, "baz")),
    // (3, "foo") causes timeout
    (count, value) => Future(doA(count, value), if (count == 3) 15000 else 120000)(DispatcherA) flatMap { seq =>
      log.info("handing off (%s, %s)".format(count, value))
      val f = Future.traverse(seq, 120000) { v => Future(doB(v), 120000)(DispatcherB) }
      log.info("inner future created: " + f)
      f
    }
  )
  
  private def run(list: List[(Int, String)], createFuture: (Int, String) => Future[Seq[String]]): Unit = {
    list foreach { in =>
      val (count, value) = in
      val future = createFuture(count, value) onResult {
        case s => log.info("onResult: " + s)
      } onException {
        case e => log.info("onExceptiont: " + e)
      } onTimeout { f =>
        log.info("onTimeout: " + f)
      }
      log.info("outter future created: " + future)
    }
  }
  
  goodRun
//  errorRun
//  timeoutRun
  Thread.sleep(120000)
  sys.exit

}
