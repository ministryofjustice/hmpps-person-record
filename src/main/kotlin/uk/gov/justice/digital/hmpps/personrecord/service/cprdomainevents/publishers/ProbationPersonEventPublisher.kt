package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_UPDATED

@Component
class ProbationPersonEventPublisher(
  domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher(domainEventPublisher, baseUrl) {
  override val sourceSystemType = SourceSystemType.DELIUS
  override val createEventType = CPR_PROBATION_PERSON_CREATED
  override val updateEventType = CPR_PROBATION_PERSON_UPDATED
  override val path = "/person/probation/"
  override val createdDescription = "A probation person record has been created"
  override val updatedDescription = "A probation person record has been updated"
  override val sourceSystemIdField = "CRN"
}
