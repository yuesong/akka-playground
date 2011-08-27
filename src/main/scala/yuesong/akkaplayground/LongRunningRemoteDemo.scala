package yuesong.akkaplayground
import akka.actor._
import akka.actor.Actor._
import akka.util.duration._
import yuesong.util.Helper

/**
 * This is a demon for the following issue regarding remote actors:
 * 
 * Client sends multiple messages to an actor, then blocks and waits for the replies. If the actor is a local one, the 
 * client gets the reply to each message as soon as it is processed. If the actor is a remote one, however, the 
 * client won't get any reply until all messages are processed, at which time it receives all replies at the same time.
 */
object ClientApp extends App with Helper {
  
  val localActor = actorOf[MyActor].start
  info("Sending multiple messages to a local actor. Should recieve a reply as soon as the message is processed")
  sendCmds(localActor, DoSomething(2000), DoSomething(3000), DoSomething(4000))
  
  val remoteActor = remote.actorFor("do-something-service", "localhost", 2552)
  info("Sending multiple messages to a remote actor. Replies are not received until all messages are processed")
  sendCmds(remoteActor, DoSomething(2000), DoSomething(3000), DoSomething(4000))
  
  remote.shutdown
  registry.shutdownAll

  def sendCmds(actor: ActorRef, cmds: DoSomething*) {
    cmds.par foreach { cmd =>
      info("Sending " + cmd)
      info("Received " + actor.?(cmd)(timeout = 60 seconds).as[Any])
    }
  }
}

object ServerApp extends App with Helper {
  remote.start("0.0.0.0", 2552)
  remote.register("do-something-service", actorOf[MyActor])
}

class MyActor extends Actor with Helper {
  def receive = {
    case cmd: DoSomething =>
      info("Received " + cmd)
      Thread.sleep(cmd.delay)
      val done = Done(cmd)
      self reply done
      info("Replied " + done)
  }
}

case class DoSomething(delay: Long, msg: Any = Helper.now)

case class Done(cmd: DoSomething)
