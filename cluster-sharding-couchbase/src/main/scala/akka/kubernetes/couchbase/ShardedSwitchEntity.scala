package akka.kubernetes.couchbase

import akka.actor.{ActorRef, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}

import scala.math.abs

object ShardedSwitchEntity extends ExtensionId[ShardedSwitchEntity] with ExtensionIdProvider {
  override def lookup: EventProcessorWrapper.type = EventProcessorWrapper

  override def createExtension(system: ExtendedActorSystem) = new ShardedSwitchEntity(system)

  case class EntityEnvelope(switchEntityId: String, payload: Any)

  val extractEntityId: ShardRegion.ExtractEntityId = {
    case EntityEnvelope(id, msg) => (id, msg)
  }

  def extractShardId(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case EntityEnvelope(eventProcessorId, _) => abs(eventProcessorId.hashCode % numberOfShards).toString
  }
}

class ShardedSwitchEntity(system: ExtendedActorSystem) extends Extension {

  import ShardedSwitchEntity._

  private val switchSettings = Settings(system).switchSettings

  private val typeName = switchSettings.id

  private val shardCount = switchSettings.shardCount

  def start(): Unit =
    ClusterSharding(system).start(
      typeName = typeName,
      entityProps = SwitchEntity.props,
      settings = ClusterShardingSettings(system).withRole("write-side"),
      extractEntityId = extractEntityId,
      extractShardId = extractShardId(shardCount)
    )

  def tell(id: String, msg: Any)(implicit sender: ActorRef = ActorRef.noSender): Unit =
    shardRegion ! EntityEnvelope(id, msg)

  def shardRegion: ActorRef = ClusterSharding(system).shardRegion(typeName)

}
