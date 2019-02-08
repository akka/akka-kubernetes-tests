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
  dockerUsername := Some("akka-kubernetes-tests"),
  dockerCommands ++= Seq(
    Cmd("USER", "root"),
    Cmd("RUN", "/sbin/apk", "add", "--no-cache", "bash", "bind-tools", "busybox-extras", "curl", "iptables"),
    Cmd("RUN", "chgrp -R 0 . && chmod -R g=u .")
  ),
  dockerUpdateLatest := true,
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
        resolvers += Resolver.bintrayRepo("akka", "maven"),
        resolvers += Resolver.url("lightbend-commercial", url("https://repo.lightbend.com/commercial-releases"))(Resolver.ivyStylePatterns)
      )
    )
  ).aggregate(`cluster-sharding`, `cluster-sharding-couchbase`)

lazy val `cluster-sharding` = (project in file("cluster-sharding"))
  .enablePlugins(JavaServerAppPackaging)
  .configs(IntegrationTest)
  .settings(
    name := "akka-kubernetes",
    libraryDependencies ++= ServiceDeps,
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

lazy val `cluster-sharding-couchbase` = (project in file("cluster-sharding-couchbase"))
  .enablePlugins(JavaServerAppPackaging)
  .configs(IntegrationTest)
  .settings(
    name := "akka-kubernetes-couchbase",
    libraryDependencies ++= CouchbaseDeps,
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

lazy val `chaos-cluster` = (project in file("chaos-cluster"))
  .enablePlugins(JavaServerAppPackaging)
  .configs(IntegrationTest)
  .settings(
    name := "chaos-cluster",
    libraryDependencies ++= ServiceDeps,
  )
  .settings(commonItTestSettings)
  .settings(commonDockerSettings)

