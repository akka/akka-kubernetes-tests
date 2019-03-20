package akka.kubernetes.soak
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import spray.json.DefaultJsonProtocol
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

// collect your json format instances into a support trait:
trait StatsJsonSupport extends SprayJsonSupport with DefaultJsonProtocol {
  implicit val testResultFormat = jsonFormat2(TestResult)
  implicit val testResultsFormat = jsonFormat7(TestResults)
}

class StatsEndpoint(system: ActorSystem, client: ActorRef) extends Directives with StatsJsonSupport {
  private implicit val askTimeout = Timeout(5.seconds)
  private val log = Logging(system, getClass)

  val route: Route =
    path("stats") {
      get {
        onComplete(client.ask(GetTestResults()).mapTo[TestResults]) {
          case Failure(t) =>
            log.error(t, "Failed to get test results")
            complete(StatusCodes.InternalServerError)
          case Success(value) =>
            complete(value)
        }
      }
    }

}
