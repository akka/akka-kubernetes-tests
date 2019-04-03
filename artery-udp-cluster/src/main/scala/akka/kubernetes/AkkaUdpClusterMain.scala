package akka.kubernetes

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.stream.ActorMaterializer

object AkkaUdpClusterMain extends App {
  implicit val system = ActorSystem("ArteryUdp")

  implicit val mat = ActorMaterializer()
  implicit val cluster = Cluster(system)

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  Cluster(system).registerOnMemberUp({
    system.log.info("Cluster member is up!")
  })

}
