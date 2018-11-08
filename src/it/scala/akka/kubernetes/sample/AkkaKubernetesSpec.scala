package akka.kubernetes.sample

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.management.cluster.{ClusterHttpManagementJsonProtocol, ClusterMembers}
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class AkkaKubernetesSpec extends WordSpec with BeforeAndAfterAll with ScalaFutures with Matchers with ClusterHttpManagementJsonProtocol with Eventually {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit override val patienceConfig = PatienceConfig(timeout = Span(20, Seconds), interval = Span(500, Millis))

  val target = System.getProperty("akka.k8s.target", "http://localhost:8080")
  val clusterSize = System.getProperty("akka.k8s.cluster-size", "1").toInt
  val deployedVersion = System.getProperty("akka.k8s.deployment-version", "LOCAL")

  val log = system.log

  log.info("Running with target {} clusterSize {} version {}", target, clusterSize, deployedVersion)

  "Version deployed" should {
    "should have been updated" in {
      eventually {
        val response = Http().singleRequest(HttpRequest(uri = s"$target/version")).futureValue
        Unmarshal(response.entity).to[String].futureValue shouldEqual deployedVersion
      }
    }
  }

  "Cluster formation" should {

    "work" in {
      eventually {
        val response = Http().singleRequest(HttpRequest(uri = s"$target/cluster/members")).futureValue
        response.status shouldEqual StatusCodes.OK

        val clusterMembers: ClusterMembers = Unmarshal(response).to[ClusterMembers].futureValue
        withClue("Latest response: " + clusterMembers) {
          clusterMembers.members.size shouldEqual clusterSize
          clusterMembers.unreachable shouldEqual Seq.empty
        }
      }
    }
  }

  "Akka Boss (singleton)" should {

    "say hello" in {
      val response = Http().singleRequest(HttpRequest(uri = s"$target/boss")).futureValue
      response.status shouldEqual StatusCodes.OK
    }
  }

  "Akka members (sharding)" should {
    "do some work" in {
      val response = Http().singleRequest(HttpRequest(uri = s"$target/team-member/johan")).futureValue
      response.status shouldEqual StatusCodes.OK
    }
  }

  override protected def afterAll(): Unit = {
    system.terminate()
  }
}
