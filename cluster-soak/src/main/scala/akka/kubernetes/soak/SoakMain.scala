package akka.kubernetes.soak

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
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

  val listeningOn = RARP(system).provider.getDefaultAddress.host.getOrElse("0.0.0.0")
  log.info("Listening on {}", listeningOn)

  Cluster(system).registerOnMemberUp({
    system.actorOf(PingPong.serverProps(), "server")
    val client = system.actorOf(PingPong.clientProps(), "client")
    val clusterStats = new StatsEndpoint(system, client)
    log.info("Cluster member is up! Starting tests and binding http server")
    Http().bindAndHandle(clusterStats.route, listeningOn, 8080)
  })

}
