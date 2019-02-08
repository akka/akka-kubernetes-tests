/*
 * Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>
 */

package akka.kubernetes.sample

import akka.cluster.{Cluster, Member, MemberStatus}
import akka.management.cluster.{
  ClusterHttpManagementJsonProtocol,
  ClusterMembers,
  ClusterReadViewAccess,
  ClusterUnreachableMember
}

// Just want the read view
object ClusterStateRoute extends ClusterHttpManagementJsonProtocol {

  import akka.http.scaladsl.server.Directives._
  import akka.management.cluster.ClusterHttpManagementHelper._

  def routeGetMembers(cluster: Cluster) =
    path("cluster" / "members") {
      get {
        complete {
          val readView = ClusterReadViewAccess.internalReadView(cluster)
          val members = readView.state.members.map(memberToClusterMember)

          val unreachable = readView.reachability.observersGroupedByUnreachable.toSeq
            .sortBy(_._1)
            .map {
              case (subject, observers) ⇒
                ClusterUnreachableMember(s"${subject.address}", observers.toSeq.sorted.map(m ⇒ s"${m.address}").toList)
            }
            .toList

          val thisDcMembers =
            cluster.state.members.toSeq
              .filter(node => node.status == MemberStatus.Up && node.dataCenter == cluster.selfDataCenter)

          val leader = readView.leader.map(_.toString)

          val oldest = if (thisDcMembers.isEmpty) None else Some(thisDcMembers.min(Member.ageOrdering).address.toString)

          ClusterMembers(s"${readView.selfAddress}", members, unreachable, leader, oldest, oldestPerRole(thisDcMembers))
        }
      }
    }

}
