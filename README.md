# Akka Cluster Tests

Basic tests for Akka Cluster and friends on Kubernetes

For each pull requests from this repository (not forks) and commits to master the following happens in travis:

* Docker image is published to https://hub.docker.com/r/kubakka/akka-kubernetes/
* A deployment is triggered to Lightbend's internal OpenShift cluster (https://centralpark.lightbend.com/)
* Integration tests are run with `sbt it:test`

See `.travis.yml` for full details.

The tests exercise:

* Cluster formation via Akka bootstrap using the Kubernetes API for service discovery
* Cluster singletons
* Cluster sharding







