# Akka Cluster with Sharding and Couchbase Persistence

This project relies on the following:

* Being deployed the namespace `akka-couchbase` (can be changed by modifying the Kubernetes resources)
* A Couchbase cluster available under the `akka-couchbase-cluster` via DNS.
* A secret named `akka-couchbase-cluster-auth` with the Couchbase `username` and `password`

One way of achieving this is using the Couchbase Open Shift / Kubernetes Operator. Setting this up requires cluster admin
privileges, instructions can be found [here](https://docs.couchbase.com/operator/1.1/install-openshift.html) 

Assuming that the operator is available in the `akka-couchbase-cluster` namespace then a cluster can be crated with the `kubernetes/couchbase/couchbase-cluster.yml`.

```
oc apply -f https://docs.couchbase.com/operator/1.1/install-openshift.html
```

The cluster is not exposed externally. To access the console port forwarding can be used:

```
oc port-forward akka-couchbase-cluster-0000 18091:8091
```

Then it can be accessed [http://localhost:18091](http://localhost:18091)

## Running the application

The application forms a single custer with multiple roles. Each role has a 








