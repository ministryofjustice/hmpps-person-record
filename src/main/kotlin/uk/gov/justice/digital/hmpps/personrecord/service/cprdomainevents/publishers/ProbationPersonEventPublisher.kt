package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.queue.DomainEventPublisher
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.CPR_PROBATION_PERSON_DELETED

@Component
class ProbationPersonEventPublisher(
  domainEventPublisher: DomainEventPublisher,
  @Value($$"${core-person-record.base-url}") private val baseUrl: String,
) : PersonEventPublisher(domainEventPublisher, baseUrl) {
  override val sourceSystemType = SourceSystemType.DELIUS
  override val createEventType = CPR_PROBATION_PERSON_CREATED
  override val deleteEventType = CPR_PROBATION_PERSON_DELETED
  override val path = "/person/probation/"
  override val createDescription = "A probation person record has been created"
  override val deleteDescription = "A probation person record has been deleted"
  override val sourceSystemIdField = "CRN"
}
