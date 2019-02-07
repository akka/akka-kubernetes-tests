import sbt._

object Dependencies {

  val AkkaVersion = "2.5.18"
  val AkkaManagementVersion = "0.20.0"
  val AkkaPersistenceCouchbaseVersion = "1.0-RC2"
  val SplitBrainResolverVersion =  "1.1.7+18-08f41217+20190207-1340" // TODO update

  val AkkaCluster = "com.typesafe.akka" %% "akka-cluster" % AkkaVersion
  val AkkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion
  val AkkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
  val AkkaSlj4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion

  val AkkaBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion
  val AkkaServiceDiscoveryK8Api = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion
  val AkkaServiceDiscoveryConfig = "com.lightbend.akka.discovery" %% "akka-discovery-config" % AkkaManagementVersion
  val AkkaClusterHttp =  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion

  val AkkaPersistenceCouchbase = "com.lightbend.akka" %% "akka-persistence-couchbase" % AkkaPersistenceCouchbaseVersion

  val SplitBrainResolver = "com.lightbend.akka" %% "akka-split-brain-resolver" % SplitBrainResolverVersion

  val Logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val ScalaTest = "org.scalatest" %% "scalatest" % "3.0.5" % "it,test"

  val ServiceDeps = Seq(
    AkkaBootstrap, AkkaServiceDiscoveryK8Api, AkkaServiceDiscoveryConfig, AkkaClusterHttp,
    AkkaCluster, AkkaClusterSharding, AkkaClusterTools, AkkaSlj4j,
    SplitBrainResolver,
    Logback,
    ScalaTest
  )

  val CouchbaseDeps = ServiceDeps ++ Seq(
    AkkaPersistenceCouchbase
  )
}

