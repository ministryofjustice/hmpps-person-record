package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED

@Component
class ProbationPersonEventPublisher(
  domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher(domainEventPublisher, baseUrl) {
  override val sourceSystemType = SourceSystemType.DELIUS
  override val identifierType = "CRN"
  override val eventType = CPR_PROBATION_PERSON_CREATED
}
