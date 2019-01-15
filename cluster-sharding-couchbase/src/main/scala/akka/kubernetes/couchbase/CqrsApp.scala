package akka.kubernetes.couchbase

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.management.cluster.bootstrap.ClusterBootstrap
import akka.management.scaladsl.AkkaManagement
import akka.persistence.couchbase.scaladsl.CouchbaseReadJournal
import akka.persistence.query.{NoOffset, PersistenceQuery}
import akka.stream.ActorMaterializer

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Random, Try}

object CqrsApp {

  def main(args: Array[String]): Unit = {
    val system = ActorSystem("CouchbaseSystem")
    val materializer = ActorMaterializer()(system)
    val ec: ExecutionContext = system.dispatcher
    val log = system.log

    AkkaManagement(system).start()
    ClusterBootstrap(system).start()

    Cluster(system).registerOnMemberUp {

      val selfRoles = Cluster(system).selfRoles

      log.info("Running with roles {}", selfRoles)

      val shardedSwitchEntity = ShardedSwitchEntity(system)
      shardedSwitchEntity.start()
      EventProcessorWrapper(system).start()

      if (selfRoles.contains("load-generation")) {
        log.info("Starting load generation")
        testIt(system, shardedSwitchEntity)
      }

      if (selfRoles.contains("simple-query")) {
        log.info("Starting simple query")
        verySimpleRead(system, materializer, ec)
      }
    }

    def verySimpleRead(implicit system: ActorSystem, mat: ActorMaterializer, ec: ExecutionContext): Unit = {
      val query = PersistenceQuery(system).readJournalFor[CouchbaseReadJournal](CouchbaseReadJournal.Identifier)
      val startTime = System.currentTimeMillis()
      query
        .currentEventsByTag("tag1", NoOffset)
        .runFold(0)((count, _) => count + 1)
        .onComplete { t: Try[Int] =>
          system.log.info("Query finished for tag1 in {}. Read {} rows",
                          (System.currentTimeMillis() - startTime).millis.toSeconds,
                          t)
        }
    }

    // Every instance will add 100 persistent actors and second 2 messages to each per 2 seconds
    def testIt(system: ActorSystem, shardedSwitch: ShardedSwitchEntity): Unit = {
      val uuid = UUID.randomUUID()
      val nrSwitches = 100
      def switchName(nr: Int) = s"switch-$uuid-$nr"
      log.info("Creating {} switches with uuid {}", nrSwitches, uuid)
      (0 until nrSwitches) foreach { s =>
        shardedSwitch.tell(switchName(s), SwitchEntity.CreateSwitch(6))
      }
      import system.dispatcher
      system.scheduler.schedule(3.seconds, 2.second) {
        (0 until nrSwitches) foreach { s =>
          val switch = switchName(s)
          log.debug("Sending messages to switch {}", switch)
          shardedSwitch.tell(switch, SwitchEntity.SetPortStatus(Random.nextInt(6), portEnabled = true))
          shardedSwitch.tell(switch, SwitchEntity.SendPortStatus)
        }
      }
    }
  }

}
