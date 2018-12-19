# Akka Cluster with Sharding and Couchbase Persistence


This project relies on the following:

* Being deployed the namespace `akka-couchbase` (can be changed by modifying the Kubernetes resources)
* A Couchbase cluster available under the `akka-couchbase-cluster` via DNS.
* A secret named `akka-couchbase-cluster-auth` with the Couchbase `username` and `password`

One way of achieving this is using the Couchbase Open Shift / Kubernetes Operator. Assuming that is available in the namespace
then a cluster can be crated with the `kubernetes/couchbase/couchbase-cluster.yml`.

## Running locally

TODO