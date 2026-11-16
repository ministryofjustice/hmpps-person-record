package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.cluster

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.ClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher

@Component
class ClusterChangedEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) {

  fun onClusterChange(clusterChanged: ClusterChanged) {
    domainEventPublisher.publish(
      CprClusterChanged(
        eventType = TODO(),
        version = TODO(),
        occurredAt = TODO(),
        description = TODO(),
        detailUrl = TODO(),
        personReference = TODO(),
      ),
    )
  }
}
