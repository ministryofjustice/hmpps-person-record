package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.SyncReconciliation
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PrisonPseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository

@Component
class SyncReconciliationGetHandler(
  private val personRepository: PersonRepository,
  private val prisonReligionRepository: PrisonReligionRepository,
  private val prisonPseudonymRepository: PrisonPseudonymRepository,
) {
  @Transactional(readOnly = true)
  fun get(prisonNumber: String): SyncReconciliation {
    val personEntity = personRepository.findByPrisonNumber(prisonNumber)
      ?: throw ResourceNotFoundException(prisonNumber)
    val prisonReligionEntities = prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(prisonNumber)
    val prisonPseudonymEntities = prisonPseudonymRepository.findAllById(personEntity.pseudonyms.mapNotNull { it.id })
    return SyncReconciliation.from(personEntity, prisonReligionEntities, prisonPseudonymEntities)
  }
}
