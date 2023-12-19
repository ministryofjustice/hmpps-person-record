package uk.gov.justice.digital.hmpps.personrecord.message.listeners.notifiers

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.DomainEvent
import java.net.URI

const val NEW_OFFENDER_CREATED = "probation-case.engagement.created"

@Component(value = NEW_OFFENDER_CREATED)
class OffenderCreatedEventProcessor(
) : EventProcessor() {
    override fun processEvent(domainEvent: DomainEvent) {
        val offenderDetailUrl = domainEvent.detailUrl
        val path = URI.create(offenderDetailUrl).path
        val crn = domainEvent.personReference?.identifiers?.first{it.type == "CRN"}
        LOG.debug("Enter processEvent with  Info:$offenderDetailUrl")
    }
}
