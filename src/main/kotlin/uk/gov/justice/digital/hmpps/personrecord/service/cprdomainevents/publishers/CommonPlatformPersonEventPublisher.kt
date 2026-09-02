package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_UPDATED

@Profile("!preprod & !prod")
@Component
class CommonPlatformPersonEventPublisher(
  domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher(domainEventPublisher, baseUrl) {
  override val sourceSystemType = SourceSystemType.COMMON_PLATFORM
  override val createEventType = CPR_COURT_PERSON_CREATED
  override val updateEventType = CPR_COURT_PERSON_UPDATED
  override val path = "/person/commonplatform/"
  override val createdDescription = "A court person record has been created"
  override val updatedDescription = "A court person record has been updated"
  override val sourceSystemIdField = "DEFENDANT_ID"
}
