/*
 * Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.kubernetes.sample

import akka.actor.ActorRef
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.kubernetes.sample.AkkaMember.Hello
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

class TalkToATeamMemberRoute(sharding: ActorRef) {
  implicit val timeout = Timeout(10.second)
  def route() =
    extractActorSystem { as =>
      path("team-member" / Segment) { name =>
        get {
          val teamMemberF = (sharding ? Hello(name)).mapTo[String]
          onComplete(teamMemberF) {
            case Success(response) => complete(response)
            case Failure(ex) => complete((StatusCodes.InternalServerError, s"An error occurred: ${ex.getMessage}"))
          }
        }
      }
    }

}
