package akka.cluster.soak
import akka.actor.ActorSystem
import akka.discovery.ServiceDiscovery.Resolved
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, StatusCodes}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.kubernetes.soak.Tests.{ResponseTimeNanos, Target}
import akka.kubernetes.soak.{StatsJsonSupport, TestResults}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import akka.util.PrettyDuration._

import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration._

class ClusterSoakSpec(endpoints: Resolved)(implicit system: ActorSystem)
    extends WordSpec
    with StatsJsonSupport
    with ScalaFutures
    with Matchers {

  import system.dispatcher
  implicit val mat = ActorMaterializer()
  implicit override val patienceConfig = PatienceConfig(timeout = Span(30, Seconds), interval = Span(2, Seconds))
  val log = Logging(system, getClass)

  "The Clustered service" should {

    "not have had any failures" in {

      val responses: immutable.Seq[TestResults] = Source(endpoints.addresses)
        .mapAsyncUnordered(10) { rt =>
          log.info("Hitting {}", rt.host)
          val request = HttpRequest(uri = s"http://${rt.host}:${rt.port.getOrElse(8080)}/stats")
          for {
            response <- Http().singleRequest(request)
            entity <- response.entity.toStrict(1.second)
            results <- response.status match {
              case StatusCodes.OK =>
                Unmarshal(entity).to[TestResults]
              case unexpected =>
                Future.failed(
                  new RuntimeException(s"Unexpected response code: $unexpected body: ${entity.data.utf8String}")
                )
            }
          } yield results
        }
        .runWith(Sink.seq)
        .futureValue

      log.info("{} nodes tested", responses.size)

      val maxJoinTimes =
        responses.map(_.joiningTime).sorted.reverse.take(5).map(_.nanos.pretty)

      log.info("Max join times: {}", maxJoinTimes)

      val maxResponseTimePerNode: immutable.Seq[(Target, ResponseTimeNanos)] =
        responses.map(_.lastResult.responses.maxBy(_._2))

      val averageResponseTimesPerNode = responses
        .map((eachNode: TestResults) => {
          val total = eachNode.lastResult.responses.map(_._2).sum.nanos
          val count = eachNode.lastResult.responses.size
          total / count
        })
        .sorted
        .reverse

      log.info("All response times: {}", responses)
      log.info("Slowest response times across all node pings: {}",
               maxResponseTimePerNode.sortBy(_._2).reverse.take(5).map(_._2.nanos.pretty))
      log.info("Slowest average response times across all node pings: {}",
               averageResponseTimesPerNode.take(5).map(_.pretty))

      responses.filter(_.testsFailed != 0) shouldEqual Nil

      withClue("Response took longer than 2 seconds. Do some investigation") {
        responses.filter(_.lastResult.responses.exists(_._2.nanos > 2.seconds)) shouldEqual Nil
      }

      withClue("Found unreachable events") {
        responses.filter(_.memberUnreachableEvents != 0) shouldEqual Nil
      }

      withClue("Found downed events") {
        responses.filter(_.memberDownedEvents != 0) shouldEqual Nil
      }
    }
  }

}
