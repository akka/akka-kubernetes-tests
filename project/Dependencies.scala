import com.lightbend.cinnamon.sbt.Cinnamon
import sbt._

object Dependencies {

  val AkkaVersion = "2.5.21"
  val AkkaManagementVersion = "1.0.0-RC2"
  val AkkaPersistenceCouchbaseVersion = "1.0-RC2"
  val CommercialVersion = "1.1.7+27-745cd37d"

  val AkkaCluster = "com.typesafe.akka" %% "akka-cluster" % AkkaVersion
  val AkkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % AkkaVersion
  val AkkaClusterSharding = "com.typesafe.akka" %% "akka-cluster-sharding" % AkkaVersion
  val AkkaClusterTools = "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
  val AkkaSlj4j = "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion

  val AkkaBootstrap = "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion
  val AkkaServiceDiscoveryK8Api = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % AkkaManagementVersion
  val AkkaClusterHttp =  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion

  val AkkaPersistenceCouchbase = "com.lightbend.akka" %% "akka-persistence-couchbase" % AkkaPersistenceCouchbaseVersion

  val SplitBrainResolver = "com.lightbend.akka" %% "akka-split-brain-resolver" % CommercialVersion
  val KubernetesLease = "com.lightbend.akka" %% "akka-lease-kubernetes" % CommercialVersion
  val Diagnostics = "com.lightbend.akka" %% "akka-diagnostics" % CommercialVersion

  val Logback = "ch.qos.logback" % "logback-classic" % "1.2.3"

  val ScalaTestProd = "org.scalatest" %% "scalatest" % "3.0.5"
  val ScalaTest = ScalaTestProd % "it,test"

  val ServiceDeps = Seq(
    AkkaBootstrap, AkkaServiceDiscoveryK8Api, AkkaClusterHttp, AkkaDiscovery,
    AkkaCluster, AkkaClusterSharding, AkkaClusterTools, AkkaSlj4j,
    SplitBrainResolver,
    KubernetesLease,
    Diagnostics,
    Cinnamon.library.cinnamonAkka,
    Cinnamon.library.cinnamonAkkaHttp,
    Cinnamon.library.cinnamonAkkaStream,
    Cinnamon.library.cinnamonPrometheus,
    Cinnamon.library.cinnamonPrometheusHttpServer,
    Logback,
    ScalaTest
  )

  val ClusterSoakTestDeps = Seq(
    AkkaClusterHttp,
    AkkaDiscovery,
    AkkaServiceDiscoveryK8Api,
    Diagnostics,
    Logback,
    ScalaTestProd
  )
  val CouchbaseDeps = ServiceDeps ++ Seq(
    AkkaPersistenceCouchbase
  )
}

