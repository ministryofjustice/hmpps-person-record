package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.DatabaseRetryable
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class TransactionalEthnicityUpdater(
  private val personRepository: PersonRepository,
) {

  @DatabaseRetryable
  @Transactional
  fun update(person: PersonEntity) {
    person.getPrimaryName().ethnicityCode = person.ethnicityCode
    personRepository.save(person)
  }
}
