import Dependencies._
import com.typesafe.sbt.packager.docker._

enablePlugins(JavaServerAppPackaging)

configs(IntegrationTest)
Defaults.itSettings

version in ThisBuild ~= (_.replace('+', '-'))

lazy val root = (project in file("."))
  .configs(IntegrationTest)
  .settings(
    name := "akka-kubernetes",
    Defaults.itSettings,

    libraryDependencies ++= ServiceDeps,

    dockerCommands :=
      dockerCommands.value.flatMap {
        case ExecCmd("ENTRYPOINT", args@_*) => Seq(Cmd("ENTRYPOINT", args.mkString(" ")))
        case v => Seq(v)
      },

    dockerExposedPorts := Seq(8080, 8558, 2552),
    dockerBaseImage := "openjdk:8-jre-alpine",

    dockerCommands ++= Seq(
      Cmd("USER", "root"),
      Cmd("RUN", "/sbin/apk", "add", "--no-cache", "bash", "bind-tools", "busybox-extras", "curl", "strace"),
      Cmd("RUN", "chgrp -R 0 . && chmod -R g=u .")
    ),

    dockerUsername := Some("chbatey"),
    dockerUpdateLatest := true,

    javaOptions in IntegrationTest ++= collection.JavaConverters.propertiesAsScalaMap(System.getProperties)
      .collect { case (key, value) if key.startsWith("akka") => "-D" + key + "=" + value }.toSeq,


  )



