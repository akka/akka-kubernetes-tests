# Chaos Cluster

Test:

* Nodes stopping ungracefully via endpoint that does `System.exit(-1)`)
* Nodes stopping ungracefully via `kubectl delete pods <pod> --grace-period=0 --force` 
* Nodes leaving the cluster gracefully
* Network partitions via all traffic dropped via `iptables`
* Network instabbility via `tc` 
