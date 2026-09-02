package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PRISON_PERSON_UPDATED

@Profile("!preprod & !prod")
@Component
class PrisonPersonEventPublisher(
  domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher(domainEventPublisher, baseUrl) {
  override val sourceSystemType = SourceSystemType.NOMIS
  override val createEventType = CPR_PRISON_PERSON_CREATED
  override val updateEventType = CPR_PRISON_PERSON_UPDATED
  override val path = "/person/prison/"
  override val createdDescription = "A prison person record has been created"
  override val updatedDescription = "A prison person record has been updated"
  override val sourceSystemIdField = "prisonNumber"
}
