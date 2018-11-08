/*
 * Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.kubernetes.sample

import akka.actor.ActorSystem
import akka.cluster.{Cluster, MemberStatus}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object DeploymentVersion {
  val Version: String = Option(System.getenv("VERSION")).getOrElse("LOCAL")
}

class HealthCheckRoute(system: ActorSystem) {

  private val log: LoggingAdapter = Logging(system, getClass)
  private val cluster = Cluster(system)
  private val readyStates: Set[MemberStatus] = Set(MemberStatus.Up, MemberStatus.WeaklyUp)

  val healthChecks: Route =
    concat(
      path("ready") {
        get {
          val selfState = cluster.selfMember.status
          log.debug("ready? clusterState {}", selfState)
          if (readyStates.contains(selfState)) complete(StatusCodes.OK)
          else complete(StatusCodes.InternalServerError)
        }
      },
      path("alive") {
        get {
          complete(StatusCodes.OK)
        }
      },
      path("version") {
        complete(DeploymentVersion.Version)
      }
    )
}
