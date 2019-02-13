package akka.kubernetes.soak

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.remote.RARP
import akka.stream.ActorMaterializer

object SoakMain extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  val log = system.log
  val management = AkkaManagement(system).start()
  val bootstrap = ClusterBootstrap(system).start()

  val route =
    path("hello") {
      get {
        complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hellos</h1>"))
      }
    }

  val listeningOn = RARP(system).provider.getDefaultAddress.host.getOrElse("0.0.0.0")
  log.info("Listening on {}", listeningOn)

  Http().bindAndHandle(route, listeningOn, 8080)

  Cluster(system).registerOnMemberUp({
    log.info("Cluster member is up! Starting ping pong")
    system.actorOf(PingPong.serverProps(), "server")
    system.actorOf(PingPong.clientProps(), "client")
  })

}
