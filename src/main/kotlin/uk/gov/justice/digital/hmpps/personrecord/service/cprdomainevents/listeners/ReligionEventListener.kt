package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.ReligionEventPublisher

@Component
class ReligionEventListener(religionEventPublishers: List<ReligionEventPublisher>) {

  private val publishersBySourceSystem = religionEventPublishers.associateBy { it.sourceSystemType }

  @TransactionalEventListener
  fun onReligionUpdated(religionCreated: ReligionCreated) {
    publishersBySourceSystem[SourceSystemType.NOMIS]?.onCreate(religionCreated)
  }

  @TransactionalEventListener
  fun onAddressUpdated(religionUpdated: ReligionUpdated) {
    publishersBySourceSystem[SourceSystemType.NOMIS]?.onUpdate(religionUpdated)
  }
}
