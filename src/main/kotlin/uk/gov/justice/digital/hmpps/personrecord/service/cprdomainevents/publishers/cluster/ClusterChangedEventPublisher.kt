package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.cluster

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_CLUSTER_CHANGED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.cluster.ClusterChanged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher

@Component
class ClusterChangedEventPublisher(private val domainEventPublisher: DomainEventPublisher) {

  fun onClusterChange(clusterChanged: ClusterChanged) {
    val cluster = clusterChanged.cluster
    domainEventPublisher.publish(
      CprClusterChanged(
        eventType = CPR_CLUSTER_CHANGED,
        description = "A person record match has changed",
        personReference = PersonReference(
          cluster.personEntities.map {
            val identifier = it.extractSourceSystemIdByType()
            PersonIdentifier(
              type = identifier.first,
              value = identifier.second,
            )
          },
        ),
      ),
    )
  }
}
