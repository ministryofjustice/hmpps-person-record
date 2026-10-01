package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CPR_PROBATION_PERSON_MERGED
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher

@Component
class ProbationPersonMergedEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonMergedEventPublisher {
  override val sourceSystemType = SourceSystemType.DELIUS

  override fun onMerge(personMerged: PersonMerged) {
    val fromCrn = personMerged.from!!.crn!!
    val toCrn = personMerged.to.crn!!

    domainEventPublisher.publish(
      CprPersonMerged(
        eventType = CPR_PROBATION_PERSON_MERGED,
        description = "A probation person record has been merged",
        detailUrl = "$baseUrl/person/probation/$toCrn",
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("fromCRN", fromCrn),
            PersonIdentifier("toCRN", toCrn),
          ),
        ),
      ),
    )
  }
}
