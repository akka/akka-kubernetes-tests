/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.kubernetes.sample

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.cluster.ClusterEvent.ClusterDomainEvent
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings, ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.cluster.{Cluster, ClusterEvent}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.management.AkkaManagement
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.stream.ActorMaterializer

object DemoApp extends App {

  implicit val system = ActorSystem("Kubernetessample")

  import system.{dispatcher, log}

  implicit val mat = ActorMaterializer()
  implicit val cluster = Cluster(system)

  log.info("Running with [{}]", new Resources())
  log.info(s"Started [$system], cluster.selfAddress = ${cluster.selfAddress}")

  AkkaManagement(system).start()
  ClusterBootstrap(system).start()

  system.actorOf(
    ClusterSingletonManager.props(
      singletonProps = Props(new AkkaBoss("patriknw")),
      terminationMessage = PoisonPill,
      settings = ClusterSingletonManagerSettings(system)), "boss")

  val bossProxy = system.actorOf(
    ClusterSingletonProxy.props(
      singletonManagerPath = "/user/boss",
      settings = ClusterSingletonProxySettings(system)),
    name = "bossProxy")

  val teamMembers = ClusterSharding(system).start(
    "team-member",
    Props(new AkkaMember()),
    ClusterShardingSettings(system),
    AkkaMember.extractEntityId,
    AkkaMember.extractShardId
  )

  cluster.subscribe(system.actorOf(Props[ClusterWatcher]), ClusterEvent.InitialStateAsEvents, classOf[ClusterDomainEvent])

  val talkToTheBoss = new TalkToTheBossRouteRoute(bossProxy)
  val talkToATeamMember = new TalkToATeamMemberRoute(teamMembers)

  Http().bindAndHandle(concat(talkToTheBoss.route(), talkToATeamMember.route(), ClusterStateRoute.routeGetMembers(cluster)), "0.0.0.0", 8080)

  Cluster(system).registerOnMemberUp({
    log.info("Cluster member is up!")
  })


}

class ClusterWatcher extends Actor with ActorLogging {
  implicit val cluster = Cluster(context.system)

  override def receive = {
    case msg ⇒ log.info(s"Cluster ${cluster.selfAddress} >>> " + msg)
  }
}
