package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_COURT_PERSON_CREATED

@Component
class CommonPlatformPersonEventPublisher(
  domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher(domainEventPublisher, baseUrl) {
  override val sourceSystemType = SourceSystemType.COMMON_PLATFORM
  override val identifierType = "DEFENDANT_ID"
  override val eventType = CPR_COURT_PERSON_CREATED
}
