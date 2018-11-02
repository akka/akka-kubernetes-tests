import sbt._

object Dependencies {

  val AkkaVersion = "2.5.17"
  // TODO not published
  val AkkaManagementVersion = "0.18.0+29-56d7b15b+20181101-1725"

  val AkkaCluster = "com.typesafe.akka" %% "akka-cluster" % AkkaVersion
  val AkkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion
  val AkkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
  val AkkaSlj4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion

  val AkkaBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion
  val AkkaServiceDiscoveryK8Api = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion
  val AkkaServiceDiscoveryConfig = "com.lightbend.akka.discovery" %% "akka-discovery-config" % AkkaManagementVersion
  val AkkaClusterHttp =  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion

  val Logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % "it,test"

  val ServiceDeps = Seq(
    AkkaBootstrap, AkkaServiceDiscoveryK8Api, AkkaServiceDiscoveryConfig, AkkaClusterHttp,
    AkkaCluster, AkkaClusterSharding, AkkaClusterTools, AkkaSlj4j,
    Logback,
    ScalaTest
  )
}
