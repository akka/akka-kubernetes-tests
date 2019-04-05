package akka.cluster.soak
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.discovery.{Discovery, Lookup}
import akka.dispatch.Dispatchers
import akka.stream.ActorMaterializer
import com.lightbend.akka.diagnostics.{StarvationDetector, StarvationDetectorSettings}
import org.scalatest._
import org.scalatest.events.{Event, TestFailed}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TestSuccess extends Reason
object TestFailure extends Reason
object TestException extends Reason

object ClusterSoakMain extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  import system.dispatcher
  val log = system.log

  system.log.info("Starting cluster soak tests")

  val serviceDiscovery = Discovery(system).discovery
  val resolveTimeout = 5.seconds

  val dnsDispatcher = system.dispatchers.lookup("dns-dispatcher")
  StarvationDetector.checkExecutionContext(dnsDispatcher, system.log, StarvationDetectorSettings(
    checkInterval = 1.second,
    initialDelay = 5.seconds,
    maxDelayWarningThreshold = 100.millis,
    warningInterval = 10.seconds), () => false)

  @volatile var failed = false
  val testResult = for {
    endpoints <- serviceDiscovery.lookup(Lookup("cluster-soak").withPortName("http").withProtocol("tcp"),
                                         resolveTimeout)
  } yield {
    log.info("Endpoints {}", endpoints)
    val reporter = new Reporter() {
      override def apply(event: Event): Unit =
        event match {
          case tf: TestFailed =>
            failed = true
            log.error("TestFailed({}): {}", tf.testName, tf.message)
          case _ =>
        }
    }
    new ClusterSoakSpec(endpoints).run(
      None,
      Args(reporter, Stopper.default, Filter()),
    )
  }

  testResult.onComplete {
    case Success(r) =>
      val result = if (r.succeeds()) TestSuccess else TestFailure
      log.info("Status: {}. Success: {}. Result {}", r, r.succeeds(), result)
      CoordinatedShutdown(system).run(result)
    case Failure(t) =>
      log.error(t, "Failed to run tests")
      CoordinatedShutdown(system).run(TestException)
  }
}
