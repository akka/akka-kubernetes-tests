package akka.kubernetes.sample

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.management.cluster.{ClusterHttpManagementJsonProtocol, ClusterMembers}
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

class AkkaKubernetesSpec extends WordSpec with BeforeAndAfterAll with ScalaFutures with Matchers with ClusterHttpManagementJsonProtocol with Eventually {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  implicit override val patienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(2, Seconds))

  val target = system.settings.config.getString("akka.k8s.target")
  val clusterSize = system.settings.config.getInt("akka.k8s.cluster-size")
  val deployedVersion = system.settings.config.getString("akka.k8s.deployment-version")

  val log = system.log

  log.info("Running with target {} clusterSize {} version {}", target, clusterSize, deployedVersion)

  "Version deployed" should {
    "should have been updated" in {
      eventually {
        val response = Http().singleRequest(HttpRequest(uri = s"$target/version")).futureValue
        val reportedVersion = Unmarshal(response.entity).to[String].futureValue
        log.info("Reported version is: {}", reportedVersion)
        reportedVersion shouldEqual deployedVersion
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
        log.info("Current cluster members: {}", clusterMembers)
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
