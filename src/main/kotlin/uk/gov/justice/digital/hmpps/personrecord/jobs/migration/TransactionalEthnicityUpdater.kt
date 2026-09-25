package uk.gov.justice.digital.hmpps.personrecord.jobs.migration

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class TransactionalEthnicityUpdater(
  private val personRepository: PersonRepository,
) {

  @Transactional
  fun update(personId: Long) {
    val person = personRepository.findById(personId).orElse(null) ?: return
    person.getPrimaryName().ethnicityCode = person.ethnicityCode
    personRepository.save(person)
  }
}
