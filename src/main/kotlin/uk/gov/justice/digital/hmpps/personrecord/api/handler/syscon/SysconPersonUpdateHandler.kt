package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconUpdatePersonResponse
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService

@Component
class SysconPersonUpdateHandler(
  private val personRepository: PersonRepository,
  private val personService: PersonService,
) {

  fun handle(prisonNumber: String, prisoner: Prisoner): SysconUpdatePersonResponse = personRepository.findByPrisonNumber(prisonNumber)?.let {
    val updatedPersonEntity = personService.processPerson(Person.from(prisoner, prisonNumber)) { it }
    SysconUpdatePersonResponse.from(updatedPersonEntity)
  } ?: throw ResourceNotFoundException("Prisoner not found $prisonNumber")
}
