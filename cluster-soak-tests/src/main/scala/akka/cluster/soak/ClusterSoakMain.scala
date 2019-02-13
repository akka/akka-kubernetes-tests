package akka.cluster.soak
import akka.actor.CoordinatedShutdown.Reason
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.stream.ActorMaterializer

import scala.util.{Failure, Success, Try}

object TestSuccess extends Reason
object TestFailure extends Reason

object ClusterSoakMain extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  val log = system.log

  system.log.info("Starting cluster soak tests")

  Try(new ClusterSoakSpec { override def system: ActorSystem = system }.execute()) match {
    case Success(_) =>
      log.info("Tests passed")
      CoordinatedShutdown(system).run(TestSuccess)
    case Failure(t) =>
      log.error(t, "tests failed")
      CoordinatedShutdown(system).run(TestFailure)
  }
}
