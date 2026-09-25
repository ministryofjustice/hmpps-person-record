package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.unmerge.PersonUnmerged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_UNMERGED

@Component
class ProbationPersonUnmergedEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonUnmergedEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onUnmerge(personUnmerged: PersonUnmerged) {
    val fromCrn = personUnmerged.reactivatedRecord.crn!!
    val toCrn = personUnmerged.unmergedRecord.crn!!

    domainEventPublisher.publish(
      CprPersonUnmerged(
        eventType = CPR_PROBATION_PERSON_UNMERGED,
        description = "A probation person record has been unmerged",
        detailUrl = "$baseUrl/person/probation/$fromCrn",
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("reactivatedCRN", fromCrn),
            PersonIdentifier("unmergedCRN", toCrn),
          ),
        ),
      ),
    )
  }
}
