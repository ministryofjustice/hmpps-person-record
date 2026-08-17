package uk.gov.justice.digital.hmpps.personrecord.api.handler.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService

@Component
class ProbationOverrideHandler(
  private val overrideService: OverrideService,
  private val personRepository: PersonRepository,
  private val reclusterService: ReclusterService,
) {
  fun assignIncludeOverrideAndRecluster(defendant: PersonEntity, offender: PersonEntity) {
    if (recordsAreIncluded(defendant, offender)) {
      return
    }

    overrideService.systemInclude(defendant, offender)
    personRepository.saveAll(listOf(defendant, offender))
    offender.personKey?.let { reclusterService.recluster(offender) }
  }

  private fun recordsAreIncluded(defendant: PersonEntity, offender: PersonEntity): Boolean = defendant.overrideMarker != null &&
    defendant.overrideMarker == offender.overrideMarker &&
    defendant.getScopes().intersect(offender.getScopes()).isNotEmpty()
}
