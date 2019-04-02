to_size=200


# TODO deal with 0 pods

while true;do
  nr_pods=`oc get pods | tail -n +2 | wc -l`
  nr_ready_pods=`oc get pods | tail -n +2 | grep "Running" | grep "1/1" |  wc -l`

  echo Current pods: $nr_pods
  echo Ready pods: $nr_ready_pods


  if [ $nr_pods -eq $nr_ready_pods ]
  then
      echo Scaling as all pods are ready
      (( scale_to = ($nr_pods * 120 / 100) + 1 ))
      echo Scaling to $scale_to or to size
      if [ $scale_to -lt $to_size ]
      then
          echo Scaling to $scale_to
          oc scale deployment --replicas $scale_to cluster-soak
      else
          echo Scaling to $to_size and then exiting
          oc scale deployment --replicas $to_size cluster-soak
          exit
      fi
  else
      echo $nr_ready_pods / $nr_pods ready, waiting
      sleep 5s
  fi
done

