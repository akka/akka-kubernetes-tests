/*
 * Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.kubernetes.sample

import akka.actor.Actor
import akka.cluster.Cluster
import akka.event.Logging
import akka.kubernetes.sample.AkkaBoss.{GoToJobCentre, JobSpec, WhatCanIDo}

object AkkaBoss {
  case class WhatCanIDo(name: String)
  case class JobSpec(roles: Set[String])
  case class GoToJobCentre(name: String)
}

class AkkaBoss(name: String) extends Actor {

  val log = Logging(this)
  val cluster = Cluster(context.system)

  log.info("The boss is up and running on Cluster Node [{}]", cluster.selfMember)

  val teamMembers = Map(
    "virtualvoid" -> Set("Any and everything"),
    "johan" -> Set("radiation", "streams"),
    "raboof" -> Set("Buy a house", "HTTP"),
    "ktoso" -> Set("Stop fruiting and come home"),
    "helena" -> Set("Stopped fruiting and came home", "Drink complexi tea")
    "chbatey" -> Set("Anything but Cassandra", "Drink coffee")
  )

  override def receive: Receive = {
    case "hello" =>
      log.info("Boss to say hello to [{}]", sender())
      sender() ! s"Hello from $name"
    case WhatCanIDo(n) =>
      teamMembers.get(n) match {
        case Some(roles) => sender() ! JobSpec(roles)
        case None => sender() ! GoToJobCentre(n)
      }
    case msg =>
      log.info("Boss just says hello, what is this? [{}]", msg)
  }
}
