package uk.gov.justice.digital.hmpps.personrecord.api.handler.probation

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.OverrideService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
@Transactional(isolation = REPEATABLE_READ)
class ProbationOverrideHandler(
  private val overrideService: OverrideService,
  private val personRepository: PersonRepository,
  private val personService: PersonService,
  private val reclusterService: ReclusterService,
) {
  fun assignIncludeOverrideAndRecluster(defendantId: String, offender: Person) {
    val defendant = retrieveDefendant(defendantId)
    val offender = personService.processPerson(offender) {
      personRepository.findByCrn(offender.crn!!)
    }
    if (recordsNotIncluded(defendant, offender)) {
      overrideService.systemInclude(defendant, offender)
      personRepository.saveAll(listOf(defendant, offender))
      offender.personKey?.let { reclusterService.recluster(offender) }
    }
  }

  private fun retrieveDefendant(defendantId: String): PersonEntity = personRepository.findByDefendantId(defendantId) ?: throw ResourceNotFoundException(defendantId)

  private fun recordsNotIncluded(defendant: PersonEntity, offender: PersonEntity): Boolean = defendant.getIncludeScopes().intersect(offender.getIncludeScopes()).isEmpty()
}
