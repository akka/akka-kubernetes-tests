/*
 * Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.kubernetes.sample

import akka.actor.{Actor, Stash}
import akka.cluster.sharding.ShardRegion
import akka.cluster.singleton.{ClusterSingletonProxy, ClusterSingletonProxySettings}
import akka.event.Logging
import akka.kubernetes.sample.AkkaBoss.{GoToJobCentre, JobSpec, WhatCanIDo}
import akka.kubernetes.sample.AkkaMember.Hello

case object AkkaMember {
  case class Hello(name: String)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case msg @ Hello(id) ⇒ (id, msg)
  }

  val numberOfShards = 100

  val extractShardId: ShardRegion.ExtractShardId = {
    case Hello(id) ⇒ math.abs(id.hashCode % numberOfShards).toString
  }
}

class AkkaMember() extends Actor with Stash {
  val bossProxy = context.system.actorOf(
    ClusterSingletonProxy.props(singletonManagerPath = "/user/boss",
                                settings = ClusterSingletonProxySettings(context.system))
  )

  val name = self.path.name
  val log = Logging(this)

  override def preStart(): Unit = {
    // TODO retry
    log.info("/me good morning {}", name)
    bossProxy ! WhatCanIDo(name)
  }

  override def receive: Receive = {
    case JobSpec(roles) =>
      log.info("I'm part of the team. I can do {}", roles)
      unstashAll()
      context.become(ready(roles))
    case GoToJobCentre(n) =>
      log.info("Seems I am not in the team :( I'll go fruit picking")
      context.stop(self)
    case _ =>
      stash()
  }

  def ready(roles: Set[String]): Receive = {
    case Hello(_) =>
      sender() ! s"hello from $name. What can I do for you? It better be in ${roles.mkString(", ")}"
    case _ =>
      sender() ! "what?"
  }
}
