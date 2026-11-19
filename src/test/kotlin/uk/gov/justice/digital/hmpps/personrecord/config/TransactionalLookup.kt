package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
@Transactional(readOnly = true)
class TransactionalLookup(private val personRepository: PersonRepository) {

  fun findByPrisonNumber(prisonNumber: String, initialise: (PersonEntity) -> Unit): PersonEntity = personRepository.findByPrisonNumber(prisonNumber)!!.also(initialise)

  fun findByPrisonNumberWithReferences(prisonNumber: String): PersonEntity = findByPrisonNumber(prisonNumber) { person ->
    for (pseudonym in person.pseudonyms) {
      for (reference in pseudonym.references) {
        reference.id
      }
    }
  }
}
