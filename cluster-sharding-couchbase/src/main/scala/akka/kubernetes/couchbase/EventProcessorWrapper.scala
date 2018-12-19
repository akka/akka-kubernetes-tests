package akka.kubernetes.couchbase

import akka.actor.{
  Actor,
  ActorLogging,
  ExtendedActorSystem,
  Extension,
  ExtensionId,
  ExtensionIdProvider,
  PoisonPill,
  Props,
  Timers
}
import akka.cluster.Cluster
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import akka.cluster.singleton.{ClusterSingletonManager, ClusterSingletonManagerSettings}
import com.typesafe.config.ConfigFactory

object EventProcessorWrapper extends ExtensionId[EventProcessorWrapper] with ExtensionIdProvider {
  override def lookup: EventProcessorWrapper.type = EventProcessorWrapper

  override def createExtension(system: ExtendedActorSystem) = new EventProcessorWrapper(system)

  case class EntityEnvelope(eventProcessorId: String, payload: Any)
}

/**
 * Keeps the configured number of event processors to consume tagged events
 *
 * @param system
 */
class EventProcessorWrapper(system: ExtendedActorSystem) extends Extension {

  import EventProcessorWrapper._

  private val eventProcessorSettings = Settings(system).eventProcessorSettings

  private val typeName = eventProcessorSettings.id

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(eventProcessorId, msg) => (eventProcessorId, msg)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case EntityEnvelope(eventProcessorId, _) => eventProcessorId
  }

  def start(): Unit = {
    val clusterShardingReferenceConfig =
      ConfigFactory.load("reference.conf").getConfig("akka.cluster.sharding")
    // Use reference configuration for akka.cluster.sharding with 'rebalance-threshold set to 1
    val tunedClusterShardingConfig =
      ConfigFactory
        .parseString(s"least-shard-allocation-strategy.rebalance-threshold = 1")
        .withFallback(clusterShardingReferenceConfig)

    ClusterSharding(system).start(
      typeName = typeName,
      entityProps = EventProcessor.props,
      settings = ClusterShardingSettings(tunedClusterShardingConfig).withRole("read-side"),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId(eventProcessorSettings.parallelism)
    )

    val cluster = Cluster(system)

    if (cluster.selfRoles.contains("read-side")) {
      system.actorOf(
        ClusterSingletonManager.props(
          KeepAlive.props(typeName),
          PoisonPill,
          ClusterSingletonManagerSettings(system).withRole("read-side")
        ),
        s"${eventProcessorSettings.id}-keep-alive"
      )
    }

  }
}

object KeepAlive {
  case object ProbeEventProcessors
  case object Ping
  case object Pong

  def props(eventProcessorId: String): Props = Props(new KeepAlive(eventProcessorId))

}

class KeepAlive(typeName: String) extends Actor with ActorLogging with Timers {
  import KeepAlive._

  private val shardRegion = ClusterSharding(context.system).shardRegion(typeName)
  private val settings = Settings(context.system)

  override def receive: Receive = {
    case ProbeEventProcessors =>
      log.debug("Probing event processors")
      for (eventProcessorN <- 0 until settings.eventProcessorSettings.parallelism) {
        val eventProcessorId: String = s"${settings.eventProcessorSettings.tagPrefix}$eventProcessorN"
        shardRegion ! EventProcessorWrapper.EntityEnvelope(eventProcessorId, Ping)
      }
    case Pong =>
      log.debug(s"EventProcessor living at ${sender().path}")
  }

  override def preStart(): Unit = {
    super.preStart()
    log.info("Starting keep alive singleton")
    timers.startPeriodicTimer("keep-alive", ProbeEventProcessors, settings.eventProcessorSettings.keepAliveInterval)
  }
}
