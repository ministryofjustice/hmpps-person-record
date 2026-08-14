package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprReligionCreatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprReligionUpdated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.CprReligionUpdatedInfo
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.religion.ReligionUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_RELIGION_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_RELIGION_UPDATED

@Component
class PrisonReligionEventPublisher(
  private val domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : ReligionEventPublisher {
  override val sourceSystemType = SourceSystemType.NOMIS
  override fun onCreate(religionCreated: ReligionCreated) = with(religionCreated.prisonReligionEntity) {
    domainEventPublisher.publish(
      CprReligionCreated(
        eventType = CPR_PRISON_RELIGION_CREATED,
        description = "A prison religion has been created for a person",
        detailUrl = "$baseUrl/person/prison/$prisonNumber/religion/$updateId",
        additionalInformation = CprReligionCreatedInfo(religionCreated.prisonReligionEntity.updateId!!),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("prisonNumber", prisonNumber))),
      ),
      attributes = mapOf("eventSource" to religionCreated.domainEventSource.identifier),
    )
  }
  override fun onUpdate(religionUpdated: ReligionUpdated) = with(religionUpdated.prisonReligionEntity) {
    domainEventPublisher.publish(
      CprReligionUpdated(
        eventType = CPR_PRISON_RELIGION_UPDATED,
        description = "A prison religion has been updated for a person",
        detailUrl = "$baseUrl/person/prison/$prisonNumber/religion/$updateId",
        additionalInformation = CprReligionUpdatedInfo(religionUpdated.prisonReligionEntity.updateId!!),
        personReference = PersonReference(identifiers = listOf(PersonIdentifier("prisonNumber", prisonNumber))),
      ),
      attributes = mapOf("eventSource" to religionUpdated.domainEventSource.identifier),
    )
  }
}
