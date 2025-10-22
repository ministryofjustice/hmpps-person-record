package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.BrokenCluster
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.OverrideConflict
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.SelfHealed
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review.ReviewRaised
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.review.ReviewRemoved
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordClusterTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
class ClusterEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @Async
  @EventListener
  @TransactionalEventListener
  fun onBrokenCluster(event: BrokenCluster) {
    publisher.publishEvent(ReviewRaised(event.cluster))
    publisher.publishEvent(
      RecordClusterTelemetry(
        TelemetryEventType.CPR_RECLUSTER_CLUSTER_RECORDS_NOT_LINKED,
        event.cluster,
      ),
    )
  }

  @Async
  @EventListener
  @TransactionalEventListener
  fun onOverrideConflict(event: OverrideConflict) {
    publisher.publishEvent(ReviewRaised(event.cluster, event.additionalClusters))
    publisher.publishEvent(
      RecordClusterTelemetry(
        TelemetryEventType.CPR_RECLUSTER_MATCHED_CLUSTERS_HAS_EXCLUSIONS,
        event.cluster,
      ),
    )
  }

  @Async
  @EventListener
  @TransactionalEventListener
  fun onSelfHealed(event: SelfHealed) {
    publisher.publishEvent(ReviewRemoved(event.cluster))
    publisher.publishEvent(
      RecordClusterTelemetry(
        TelemetryEventType.CPR_RECLUSTER_SELF_HEALED,
        event.cluster,
      ),
    )
  }
}
