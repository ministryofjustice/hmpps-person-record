package uk.gov.justice.digital.hmpps.personrecord.service.message

import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation.REPEATABLE_READ
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class CreateUpdateService(
  private val personService: PersonService,
) {

  @Transactional(isolation = REPEATABLE_READ)
  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = runBlocking {
    return@runBlocking findPerson().exists(
      no = {
        personService.createPersonEntity(person)
      },
      yes = {
        personService.updatePersonEntity(person, it)
      },
    )
  }
}
