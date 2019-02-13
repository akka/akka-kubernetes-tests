package akka.kubernetes.soak
import akka.actor.{Actor, ActorLogging, ActorPath, ActorSelection, Props, RootActorPath, Timers}
import akka.cluster.{Cluster, MemberStatus}
import akka.kubernetes.soak.ClientActor.{ResponseTimeout, ResponseTimeoutKey, Tick, TickKey}
import akka.kubernetes.soak.PingPong.{Ping, Pong, Summary}
import akka.util.PrettyDuration._

import scala.concurrent.duration._

object PingPong {
  case class Ping(sendTime: Long = System.nanoTime())
  case class Pong(clientSendTime: Long)
  // make sure these are recorded on the same JVM
  class Summary(clientSendTime: Long, clientReceiveTime: Long = System.nanoTime()) {
    val roundTripTime = clientReceiveTime - clientSendTime
    override def toString = s"Summary(${roundTripTime.nanos.pretty})"
  }

  def serverProps() = Props(new ServerActor())
  def clientProps() = Props(new ClientActor())

}

class ServerActor extends Actor {
  override def receive: Receive = {
    case Ping(sendTime) => sender() ! Pong(sendTime)
  }
}

object ClientActor {
  case object TickKey
  case object Tick

  case object ResponseTimeoutKey
  case object ResponseTimeout
}

class ClientActor extends Actor with Timers with ActorLogging {

  val cluster = Cluster(context.system)

  timers.startSingleTimer(TickKey, Tick, 10.seconds)

  override def receive: Receive = idle

  def idle: Receive = {
    case Tick =>
      val members =
        cluster.state.members.filter(_.status == MemberStatus.Up).filter(_.uniqueAddress != cluster.selfUniqueAddress)
      log.info("Current Up members: {} from {}", members, cluster.state.members.size - 1) // -1 for self member
      if (members.nonEmpty) {
        val paths: Set[ActorPath] = members.map(m => {
          RootActorPath(m.address) / "user" / "server"
        })
        log.info("Sending to paths {}", paths)
        paths.foreach(p => context.actorSelection(p).tell(Ping(), self))
        context.become(awaitingResponses(paths))
        timers.startSingleTimer(ResponseTimeoutKey, ResponseTimeout, 10.seconds)
      } else {
        log.info("No other members in the cluster yet")
        timers.startSingleTimer(TickKey, Tick, 10.seconds)
      }

  }

  def awaitingResponses(paths: Set[ActorPath]): Receive = {
    case Pong(clientSendTime) =>
      require(paths.contains(sender().path), s"Got response from ${sender()} when expecting responses from ${paths}")
      val remainingPaths = paths - sender().path
      log.info("Pong received: {}", new Summary(clientSendTime))
      if (remainingPaths.isEmpty) {
        log.info("All responses received. Starting again in 10s")
        timers.startSingleTimer(TickKey, Tick, 10.seconds)
        context.become(idle)
      } else {
        context.become(awaitingResponses(remainingPaths))
      }
    case ResponseTimeout =>
      log.warning("Did not receive responses from {}. Will start again in 10s", paths)
      timers.startSingleTimer(TickKey, Tick, 10.seconds)
      context.become(idle)
  }
}
