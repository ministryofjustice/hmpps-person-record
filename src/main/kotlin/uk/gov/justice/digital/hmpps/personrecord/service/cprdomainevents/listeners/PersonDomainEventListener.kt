package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonDomainEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.PersonDomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import java.time.Instant

@Component
class PersonDomainEventListener(
  private val personDomainEventPublisher: PersonDomainEventPublisher,
  @Value("\${core-person-record.base-url}") private val baseUrl: String,
) {

  @EventListener
  fun onPersonCreated(personCreated: PersonCreated) {
    val personEntity = personCreated.personEntity
    val config = buildPersonCreatedDomainEventConfig(personEntity.sourceSystem) ?: return
    val sourceSystemId = personEntity.extractSourceSystemId()
    val detailUrl = "$baseUrl/person/${config.urlPathSegment}/$sourceSystemId"

    val personReference = PersonReference(
      identifiers = sourceSystemId?.let {
        listOf(
          PersonIdentifier(config.identifierName, it),
        )
      },
    )

    personDomainEventPublisher.publish(
      PersonDomainEvent(
        eventType = config.eventType,
        description = "A ${config.typeDescription} person record has been created",
        detailUrl = detailUrl,
        occurredAt = Instant.now().toString(),
        personReference = personReference,
      ),
    )
  }

  private fun buildPersonCreatedDomainEventConfig(sourceSystem: SourceSystemType): PersonDomainEventConfig? = when (sourceSystem) {
    NOMIS -> PersonDomainEventConfig(CPR_PRISON_PERSON_CREATED, "NOMS", "prison", "prison")
    DELIUS -> PersonDomainEventConfig(CPR_PROBATION_PERSON_CREATED, "CRN", "probation", "probation")
    COMMON_PLATFORM -> PersonDomainEventConfig(CPR_COURT_PERSON_CREATED, "DEFENDANT_ID", "commonplatform", "court")
    LIBRA -> PersonDomainEventConfig(CPR_COURT_PERSON_CREATED, "C_ID", "libra", "court")
    else -> null
  }
}

data class PersonDomainEventConfig(
  val eventType: String,
  val identifierName: String,
  val urlPathSegment: String,
  val typeDescription: String,
)
