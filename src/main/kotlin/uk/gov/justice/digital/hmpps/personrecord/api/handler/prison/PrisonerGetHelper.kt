package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus.MOVED_PERMANENTLY
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.net.URI

@Component
class PrisonerGetHelper(
  private val personRepository: PersonRepository,
) {

  fun <T : Any> get(prisonNumber: String, buildBody: (PersonEntity) -> T): ResponseEntity<T> {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
      ?: throw ResourceNotFoundException(prisonNumber)

    return when {
      personEntity.hasMergedIntoAnotherPerson() -> respondWithRedirect(getMergedToPrisonNumber(personEntity))
      else -> ResponseEntity.ok(buildBody(personEntity))
    }
  }

  private fun getMergedToPrisonNumber(personEntity: PersonEntity): String = personRepository.findByIdOrNull(personEntity.mergedTo!!)!!.prisonNumber!!

  private fun PersonEntity.hasMergedIntoAnotherPerson(): Boolean = !this.isNotMerged()

  private fun <T : Any> respondWithRedirect(targetPrisonNumber: String): ResponseEntity<T> = ResponseEntity.status(MOVED_PERMANENTLY)
    .location(URI("/person/prison/$targetPrisonNumber"))
    .build()
}
