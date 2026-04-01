package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.DpsPrisonRecord
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.prison.PrisonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType

@Component
class DpsPrisonGetHandler(
  private val prisonGetHelper: PrisonGetHelper,
  private val prisonReligionRepository: PrisonReligionRepository,
  private val prisonReferenceRepository: PrisonReferenceRepository,
) {

  fun get(prisonNumber: String): ResponseEntity<DpsPrisonRecord> = prisonGetHelper.get(prisonNumber) { personEntity ->
    val prisonReligionEntities =
      prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)

    DpsPrisonRecord.from(personEntity, prisonReligionEntities, personEntity.pseudonyms.toPrisonAliases())
  }

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
