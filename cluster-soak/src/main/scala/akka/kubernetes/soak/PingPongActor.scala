package akka.kubernetes.soak
import akka.actor.{Actor, ActorLogging, ActorPath, Address, Props, RootActorPath, Timers}
import akka.cluster.ClusterEvent._
import akka.cluster.{Cluster, MemberStatus}
import akka.kubernetes.soak.ClientActor._
import akka.kubernetes.soak.PingPong.{Ping, Pong, Summary}
import akka.kubernetes.soak.Tests.{ResponseTimeNanos, Target}
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
  def clientProps(joiningTime: FiniteDuration) = Props(new ClientActor(joiningTime))

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

object Tests {
  type Target = String
  type ResponseTimeNanos = Long
}

case class TestResult(notResponded: Set[Target], responses: List[(Target, ResponseTimeNanos)]) {
  override def toString: Target = s"TestResult(notResponded: $notResponded)"
}

case class GetTestResults(resetFailures: Boolean = true)

case class TestResults(testsRun: Long,
                       testsFailed: Long,
                       lastResult: TestResult,
                       recentFailures: List[TestResult],
                       memberDownedEvents: Long,
                       memberUnreachableEvents: Long,
                       joiningTime: Long) {

  override def toString =
    s"TestResults(testsRun: $testsRun, testsFailed: $testsFailed, lastResult: $lastResult, recentFailed: $recentFailures, downed: $memberDownedEvents, unreachable: $memberUnreachableEvents, joiningTime: $joiningTime)"
}

class ClientActor(joiningTime: FiniteDuration) extends Actor with Timers with ActorLogging {

  val cluster = Cluster(context.system)

  val testInterval = 20.seconds
  val keepFailures = 5

  var lastTestResult = TestResult(Set.empty, Nil)
  var failedTests: Array[TestResult] = new Array[TestResult](keepFailures)
  var failedIndex = 0
  var testsRun = 0
  var testsFailed = 0
  var memberDownedEvents = 0
  var memberUnreachableEvents = 0

  override def preStart(): Unit = {
    timers.startSingleTimer(TickKey, Tick, testInterval)
    cluster.subscribe(self,
                      initialStateMode = InitialStateAsSnapshot,
                      classOf[MemberDowned],
                      classOf[UnreachableMember])
  }

  override def receive: Receive = idle

  def idle: Receive = {
    case Tick =>
      val members =
        cluster.state.members.filter(_.status == MemberStatus.Up).filter(_.uniqueAddress != cluster.selfUniqueAddress)
      log.debug("Current Up members: {} from {}", members, cluster.state.members.size - 1) // -1 for self member
      testsRun += 1
      if (members.nonEmpty) {
        val paths: Set[ActorPath] = members.map(m => {
          RootActorPath(m.address) / "user" / "server"
        })
        log.debug("Sending to paths {}", paths)
        paths.foreach(p => context.actorSelection(p).tell(Ping(), self))
        context.become(awaitingResponses(paths, Nil).orElse(queryAndMemberEvents))
        timers.startSingleTimer(ResponseTimeoutKey, ResponseTimeout, 10.seconds)
      } else {
        log.info("No other members in the cluster yet")
        timers.startSingleTimer(TickKey, Tick, 10.seconds)
      }

  }

  def awaitingResponses(paths: Set[ActorPath], responses: List[(Address, Summary)]): Receive = {
    case Pong(clientSendTime) =>
      require(paths.contains(sender().path), s"Got response from ${sender()} when expecting responses from $paths")
      val path = sender().path
      val remainingPaths = paths - path
      val summary = new Summary(clientSendTime)
      log.debug("Pong received: {}", summary)
      val updatedResponses = (path.address, summary) :: responses
      if (remainingPaths.isEmpty) {
        reportResult(updatedResponses, Set.empty)
        timers.startSingleTimer(TickKey, Tick, testInterval)
        context.become(idle.orElse(queryAndMemberEvents))
      } else {
        context.become(awaitingResponses(remainingPaths, updatedResponses).orElse(queryAndMemberEvents))
      }
    case ResponseTimeout =>
      reportResult(responses, paths)
      timers.startSingleTimer(TickKey, Tick, testInterval)
      context.become(idle.orElse(queryAndMemberEvents))
  }

  def queryAndMemberEvents: Receive = {
    case GetTestResults(reset) =>
      sender() ! TestResults(testsRun,
                             testsFailed,
                             lastTestResult,
                             failedTests.toList.filter(_ != null),
                             memberDownedEvents,
                             memberUnreachableEvents,
                             joiningTime.toNanos)
      if (reset) {
        log.info("Resetting all stats")
        lastTestResult = TestResult(Set.empty, Nil)
        failedIndex = 0
        failedTests = new Array[TestResult](keepFailures)
        testsRun = 0
        testsFailed = 0
        memberDownedEvents = 0
        memberUnreachableEvents = 0
      }
    case UnreachableMember(member) =>
      log.warning("Member unreachable: {}", member)
      memberUnreachableEvents += 1

    case MemberDowned(member) =>
      log.warning("Member downed: {}", member)
      memberDownedEvents += 1

  }

  def reportResult(responses: List[(Address, Summary)], missing: Set[ActorPath]): Unit = {
    lastTestResult = TestResult(missing.map(_.address.toString), responses.map {
      case (a, s) => (a.toString, s.roundTripTime)
    })
    if (missing.isEmpty) {
      log.info("All responses received. Result: {}", responses)
    } else {
      testsFailed += 1
      log.warning("Did not receive responses from {}.  Received responses from {}", missing, responses)
    }
  }

  def addFailure(tr: TestResult): Unit = {
    failedTests(failedIndex) = tr
    failedIndex = (failedIndex + 1) % failedTests.length
  }

}
