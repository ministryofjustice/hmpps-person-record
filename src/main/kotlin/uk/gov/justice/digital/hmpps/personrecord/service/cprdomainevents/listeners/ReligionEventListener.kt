package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.ReligionEventPublisher

@Component
class ReligionEventListener(religionEventPublishers: List<ReligionEventPublisher>) {

  private val publishersBySourceSystem = religionEventPublishers.associateBy { it.sourceSystemType }

  @TransactionalEventListener
  fun onReligionCreated(religionCreated: ReligionCreated) {
    publishersBySourceSystem[religionCreated.sourceSystemType]?.onCreate(religionCreated)
  }

  @TransactionalEventListener
  fun onReligionUpdated(religionUpdated: ReligionUpdated) {
    publishersBySourceSystem[religionUpdated.sourceSystemType]?.onUpdate(religionUpdated)
  }
}
