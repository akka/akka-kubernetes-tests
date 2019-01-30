package akka.kubernetes.sample

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object DeploymentVersion {
  val Version: String = Option(System.getenv("VERSION")).getOrElse("LOCAL")
}

object VersionRoute {
  val versionRoute: Route =
    path("version") {
      complete(DeploymentVersion.Version)
    }
}
