package akka.kubernetes.sample

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TalkToTheBossRouteRoute(boss: ActorRef) {
  implicit val timeout = Timeout(10.second)
  def route() = {
    extractActorSystem { as =>
      path("boss") {
        get {
          val bossF = (boss ? "hello").mapTo[String]
          onComplete(bossF) {
            case Success(response) => complete(response)
            case Failure(ex) => complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
    }
  }

}
