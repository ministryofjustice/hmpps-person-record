package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprPersonMerged
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.merge.PersonMerged
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_MERGED

@Component
class PrisonPersonMergedEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonMergedEventPublisher {
  override val sourceSystemType = SourceSystemType.NOMIS

  override fun onMerge(personMerged: PersonMerged) {
    val fromPrisonNumber = personMerged.from!!.prisonNumber!!
    val toPrisonNumber = personMerged.to.prisonNumber!!

    domainEventPublisher.publish(
      CprPersonMerged(
        eventType = CPR_PRISON_PERSON_MERGED,
        description = "A prison person record has been merged",
        detailUrl = "$baseUrl/person/prison/$toPrisonNumber",
        personReference = PersonReference(
          identifiers = listOf(
            PersonIdentifier("fromPrisonNumber", fromPrisonNumber),
            PersonIdentifier("toPrisonNumber", toPrisonNumber),
          ),
        ),
      ),
    )
  }
}
