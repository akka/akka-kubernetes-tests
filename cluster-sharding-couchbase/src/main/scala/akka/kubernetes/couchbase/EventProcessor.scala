package akka.kubernetes.couchbase

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.couchbase.UUIDs
import akka.persistence.couchbase.scaladsl.CouchbaseReadJournal
import akka.persistence.query._
import akka.stream.{ActorMaterializer, KillSwitches, Materializer}
import akka.stream.scaladsl.{RestartSource, Sink}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object EventProcessor {
  def props: Props =
    Props(new EventProcessor)
}

class EventProcessor extends Actor with ActorLogging {

  private val settings = Settings(context.system)
  private val eventProcessorId = settings.eventProcessorSettings.id
  private val tag = self.path.name
  private implicit val ec: ExecutionContext = context.dispatcher
  private implicit val materializer: Materializer = ActorMaterializer()(context.system)
  private val query =
    PersistenceQuery(context.system).readJournalFor[CouchbaseReadJournal](CouchbaseReadJournal.Identifier)
  private val killSwitch = KillSwitches.shared("eventProcessorSwitch")
  override val log = super.log // eager initialization because used from inside stream

  override def preStart(): Unit = {
    super.preStart()
    log.info("Starting event processor for tag: {}", tag)
    runQueryStream()
  }

  override def postStop(): Unit = {
    super.postStop()
    killSwitch.shutdown()
  }

  def receive = {
    case KeepAlive.Ping =>
      sender() ! KeepAlive.Pong
      log.debug(
        s"Event processor(${self.path.name}) @ ${context.system.settings.config
          .getString("akka.remote.artery.canonical.hostname")}:${context.system.settings.config.getInt("akka.remote.artery.canonical.port")}"
      )

    case message =>
      log.error("Got unexpected message: {}", message)
  }

  private def runQueryStream(): Unit =
    RestartSource
      .withBackoff(minBackoff = 500.millis, maxBackoff = 20.seconds, randomFactor = 0.1) { () =>
        // TODO offsets, this just starts from the beginning each time
        query
          .eventsByTag(tag, NoOffset)
          .map { eventEnvelope: EventEnvelope =>
            val now = System.currentTimeMillis()
            val publishTime = eventEnvelope.offset match {
              case t: TimeBasedUUID => UUIDs.timestampFrom(t)
            }
            val delay = now - publishTime
            log.info(s"#Eventprocessor($tag) got $eventEnvelope. Event is {} ms delayed", delay) // You would write to Kafka here
            eventEnvelope.offset
          }
      }
      .via(killSwitch.flow)
      .runWith(Sink.ignore)

}
