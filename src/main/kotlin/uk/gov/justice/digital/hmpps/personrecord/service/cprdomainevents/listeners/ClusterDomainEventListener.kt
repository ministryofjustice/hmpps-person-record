package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.ClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.cluster.ClusterChangedEventPublisher

@Component
@Profile("!preprod & !prod")
class ClusterDomainEventListener(
  private val clusterChangedEventPublisher: ClusterChangedEventPublisher,
) {

  @TransactionalEventListener
  fun onClusterChange(clusterChanged: ClusterChanged) {
    clusterChangedEventPublisher.onClusterChange(clusterChanged)
  }
}
