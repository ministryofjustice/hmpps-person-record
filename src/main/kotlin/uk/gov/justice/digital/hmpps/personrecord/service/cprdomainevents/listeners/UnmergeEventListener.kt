package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents

@Component
class UnmergeEventListener(
  private val publisher: ApplicationEventPublisher,
) {

  @TransactionalEventListener
  fun onUnmergeEvent(personUnmerged: PersonUnmerged) {
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_UNMERGED, personUnmerged.reactivatedRecord))
    publisher.publishEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_UPDATED, personUnmerged.unmergedRecord))
  }
}
