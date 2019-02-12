# Chaos Cluster

Uses [toxiproxy](https://github.com/Shopify/toxiproxy) to very basic create network issues.

Each Akka pod has an additional container with toxiproxy. All inbound remoting traffic
goes into toxiproxy on port 25520 and it forwarded to the AKka process listening on port
2552. 

Toxiproxy
