package akka.kubernetes.soak

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.http.scaladsl.Http
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.remote.RARP
import akka.stream.ActorMaterializer
import com.sun.management.OperatingSystemMXBean
import scala.concurrent.duration._
import akka.util.PrettyDuration._

object SoakMain extends App {

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  val log = system.log

  import java.lang.management.ManagementFactory
  log.info("Java version: {}", sys.props("java.version"))
  log.info("Cores: " + Runtime.getRuntime.availableProcessors)
  log.info("Total Memory: " + Runtime.getRuntime.totalMemory / 1000000 + "Mb")
  log.info("Max Memory: " + Runtime.getRuntime.maxMemory / 1000000 + "Mb")
  log.info("Free Memory: " + Runtime.getRuntime.freeMemory / 1000000 + "Mb")

  val memorySize =
    ManagementFactory.getOperatingSystemMXBean.asInstanceOf[OperatingSystemMXBean].getTotalPhysicalMemorySize
  log.info("RAM: " + memorySize / 1000000 + "Mb")

  log.info("JAVA env vars: {}", sys.env.filterKeys(_.contains("JAVA")))
  log.info("JVM env vars: {}", sys.env.filterKeys(_.contains("JVM")))

  val management = AkkaManagement(system).start()
  val bootstrapStart = System.nanoTime()
  val bootstrap = ClusterBootstrap(system).start()

  val listeningOn = RARP(system).provider.getDefaultAddress.host.getOrElse("0.0.0.0")
  log.info("Listening on {}", listeningOn)

  Cluster(system).registerOnMemberUp({
    val joiningTime = (System.nanoTime() - bootstrapStart).nano
    system.actorOf(PingPong.serverProps(), "server")
    val client = system.actorOf(PingPong.clientProps(joiningTime), "client")
    val clusterStats = new StatsEndpoint(system, client)
    log.info("Cluster member is up! Starting tests and binding http server. Joining time: {}", joiningTime.pretty)
    Http().bindAndHandle(clusterStats.route, listeningOn, 8080)
  })

}
