package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.processors

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class PersonSearchProcessor(
  private val personRepository: PersonRepository,
) {

  fun findByCrn(crn: String): PersonEntity? = personRepository.findByCrn(crn)

  fun findByPrisonNumber(prisonNumber: String): PersonEntity? = personRepository.findByPrisonNumber(prisonNumber)

  fun findByCId(cId: String): PersonEntity? = personRepository.findByCId(cId)

  fun findByDefendantId(defendantId: String): PersonEntity? = personRepository.findByDefendantId(defendantId)
}
