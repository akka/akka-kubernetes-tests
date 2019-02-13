import Dependencies._
import com.typesafe.sbt.packager.docker._

// To be compatible with Docker tags
version in ThisBuild ~= (_.replace('+', '-'))
scalaVersion in ThisBuild := "2.12.8"

val commonDockerSettings = Seq(
  dockerCommands :=
    dockerCommands.value.flatMap {
      case ExecCmd("ENTRYPOINT", args @ _*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
      case v => Seq(v)
    },
  dockerExposedPorts := Seq(8080, 8558, 2552),
  dockerBaseImage := "openjdk:8-jre-alpine",
  dockerRepository := Some("docker-registry-default.centralpark.lightbend.com"),
  dockerCommands ++= Seq(
    Cmd("USER", "root"),
    Cmd("RUN", "/sbin/apk", "add", "--no-cache", "bash", "bind-tools", "busybox-extras", "curl", "iptables"),
    Cmd("RUN", "chgrp -R 0 . && chmod -R g=u .")
  ),
  dockerUpdateLatest := true,
)

val commonCinnamonSettings = Seq(
  cinnamon in run := true,
  cinnamon in test := true,
  cinnamonLogLevel := "INFO"
)


val commonItTestSettings = Seq(
  javaOptions in IntegrationTest ++= collection.JavaConverters
    .propertiesAsScalaMap(System.getProperties)
    .collect { case (key, value) if key.startsWith("akka") => "-D" + key + "=" + value }
    .toSeq,
) ++ Defaults.itSettings

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "akka-kubernetes-tests",
    inThisBuild(
      Seq(
        organization := "com.lightbend.akka",
        organizationName := "Lightbend Inc.",
        startYear := Some(2018),
        licenses := Seq(("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0"))),
        scalafmtOnCompile := true,
        scalacOptions ++= Seq(
          "-encoding",
          "UTF-8",
          "-feature",
          "-unchecked",
          "-deprecation",
          "-Xlint",
          "-Yno-adapted-args",
          "-Ywarn-dead-code",
          "-Xfuture"
        ),
        headerLicense := Some(
          HeaderLicense.Custom(
            """Copyright (C) 2018 Lightbend Inc. <http://www.lightbend.com>"""
          )
        ),
        credentials += Credentials(Path.userHome / ".lightbend" / "commercial.credentials"),
        resolvers += Resolver.bintrayRepo("akka", "maven"),
        resolvers += Resolver.bintrayRepo("lightbend", "commercial-releases")
      )
    )
  ).aggregate(`cluster-sharding`, `cluster-sharding-couchbase`)

lazy val `cluster-sharding` = (project in file("cluster-sharding"))
  .enablePlugins(JavaServerAppPackaging, Cinnamon)
  .configs(IntegrationTest)
  .settings(
    name := "akka-kubernetes",
    dockerUsername := Some("akka-kubernetes-tests"),
    libraryDependencies ++= ServiceDeps,
    commonCinnamonSettings
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

lazy val `cluster-sharding-couchbase` = (project in file("cluster-sharding-couchbase"))
  .enablePlugins(JavaServerAppPackaging, Cinnamon)
  .configs(IntegrationTest)
  .settings(
    name := "akka-kubernetes-couchbase",
    dockerUsername := Some("akka-couchbase"),
    libraryDependencies ++= CouchbaseDeps,
    commonCinnamonSettings
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

lazy val `chaos-cluster` = (project in file("chaos-cluster"))
  .enablePlugins(JavaServerAppPackaging, Cinnamon)
  .configs(IntegrationTest)
  .settings(
    name := "chaos-cluster",
    dockerUsername := Some("akka-kubernetes-tests"),
    libraryDependencies ++= ServiceDeps,
    commonCinnamonSettings
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

lazy val `cluster-soak` = (project in file("cluster-soak"))
  .enablePlugins(JavaServerAppPackaging, Cinnamon)
  .configs(IntegrationTest)
  .settings(
    dockerUsername := Some("akka-long-running"),
    name := "cluster-soak",
    libraryDependencies ++= ServiceDeps,
    commonCinnamonSettings
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

lazy val `cluster-soak-tests` = (project in file("cluster-soak-tests"))
  .enablePlugins(JavaServerAppPackaging)
  .configs(IntegrationTest)
  .settings(
    dockerUsername := Some("akka-long-running"),
    name := "cluster-soak-tests",
    libraryDependencies ++= ClusterSoakTestDeps
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

