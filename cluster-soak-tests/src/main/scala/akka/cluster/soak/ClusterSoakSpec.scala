package akka.cluster.soak
import akka.actor.ActorSystem
import org.scalatest.{Matchers, WordSpec}

abstract class ClusterSoakSpec extends WordSpec with Matchers {

  def system: ActorSystem

  "The Clustered service" should {
    "work" in {
      1 shouldEqual 2
    }
  }

}
