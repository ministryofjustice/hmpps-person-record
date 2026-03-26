package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus.MOVED_PERMANENTLY
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonCanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import java.net.URI

@Component
class PrisonGetHandler(
  private val personRepository: PersonRepository,
  private val prisonReligionRepository: PrisonReligionRepository,
  private val prisonReferenceRepository: PrisonReferenceRepository,
) {

  fun get(prisonNumber: String): ResponseEntity<PrisonCanonicalRecord> {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
    return when {
      personEntity == null -> throw ResourceNotFoundException(prisonNumber)
      personEntity.hasMergedIntoAnotherPerson() -> respondWithRedirect(getMergedToPrisonNumber(personEntity))
      else -> {
        ResponseEntity.ok(
          PrisonCanonicalRecord.from(
            personEntity = personEntity,
            prisonReligionEntities = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber),
            prisonAliases = personEntity.pseudonyms.toPrisonAliases(),
          ),
        )
      }
    }
  }

  private fun getMergedToPrisonNumber(personEntity: PersonEntity): String = personRepository.findByIdOrNull(personEntity.mergedTo!!)!!.prisonNumber!!

  private fun PersonEntity.hasMergedIntoAnotherPerson() = !this.isNotMerged()

  private fun respondWithRedirect(targetPrisonNumber: String): ResponseEntity<PrisonCanonicalRecord> = ResponseEntity
    .status(MOVED_PERMANENTLY)
    .location(URI("/person/prison/$targetPrisonNumber"))
    .build()

  private fun MutableList<PseudonymEntity>.toPrisonAliases() = this
    .associateWith { pseudonymEntity -> prisonReferenceRepository.findAllByPseudonym(pseudonymEntity) }
    .map { (pseudonymEntity, prisonReferenceEntities) ->
      PrisonAlias(
        titleCode = pseudonymEntity.titleCode,
        firstName = pseudonymEntity.firstName,
        middleNames = pseudonymEntity.middleNames,
        lastName = pseudonymEntity.lastName,
        dateOfBirth = pseudonymEntity.dateOfBirth,
        sexCode = pseudonymEntity.sexCode,
        isPrimary = pseudonymEntity.nameType == NameType.PRIMARY,
        identifiers = prisonReferenceEntities.map {
          PrisonIdentifier(
            type = it.identifierType,
            value = it.identifierValue,
            comment = it.comment,
          )
        },
      )
    }
}
