package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.cluster

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.ClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher

@Component
class ClusterChangedEventPublisher(private val domainEventPublisher: DomainEventPublisher) {

  fun onClusterChange(clusterChanged: ClusterChanged) = domainEventPublisher.publish(
    CprClusterChanged(
      description = "A person record match has changed",
      personReference = PersonReference(
        clusterChanged.cluster.personEntities.map { it.extractPersonIdentifier() },
      ),
    ),
  )
}
