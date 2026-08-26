package uk.gov.justice.digital.hmpps.personrecord.api.handler.probation

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService

@Component
class LinkCourtAndProbationRecordsHandler(
  private val overrideService: OverrideService,
  private val personRepository: PersonRepository,
  private val reclusterService: ReclusterService,
) {
  fun linkAndRecluster(defendant: PersonEntity, offender: PersonEntity) {
    overrideService.systemInclude(defendant, offender)
    personRepository.saveAll(listOf(defendant, offender))
    reclusterService.recluster(offender)
  }
}
