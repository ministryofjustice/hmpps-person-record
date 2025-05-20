package uk.gov.justice.digital.hmpps.personrecord.service.person.factory.mixin

import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.person.factory.PersonContext

interface SearchMixin {
  val context: PersonContext
  val personRepository: PersonRepository

  fun findByCrn(crn: String): SearchMixin {
    context.personEntity = personRepository.findByCrn(crn)
    return this
  }

  fun findByPrisonNumber(prisonNumber: String): SearchMixin {
    context.personEntity = personRepository.findByPrisonNumber(prisonNumber)
    return this
  }

  fun findByCId(cId: String): SearchMixin {
    context.personEntity = personRepository.findByCId(cId)
    return this
  }

  fun findByDefendantId(defendantId: String): SearchMixin {
    context.personEntity = personRepository.findByDefendantId(defendantId)
    return this
  }
}