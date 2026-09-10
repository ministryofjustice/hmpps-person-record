package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonPerson
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class PrisonEventProcessor2(
  private val personService: PersonService,
  private val personRepository: PersonRepository,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processEvent(prisonNumber: String, prisonPerson: PrisonPerson) {
    val personEntity = PersonEntity.new(
      sourceSystemType = SourceSystemType.NOMIS,
    )
    personEntity.prisonNumber = prisonNumber
    val saved = personRepository.save(personEntity)
    // now do the migration for aliases
    // now do the migration for identifiers
    personService.processPerson(Person.from(saved)) { saved } // This will do an update.
  }
}
