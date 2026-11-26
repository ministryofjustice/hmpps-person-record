package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PseudonymRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.ALIAS
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import java.util.UUID

@Component
class SyncPrimaryNameHandler(
  private val pseudonymRepository: PseudonymRepository,
  private val personService: PersonService,
) {

  @Transactional
  fun handleSetPrimaryName(prisonNumber: String, cprAliasId: String) {
    val newPrimaryName = pseudonymRepository.findByUpdateId(UUID.fromString(cprAliasId))
      ?: throw ResourceNotFoundException("Alias with $cprAliasId not found for person with $prisonNumber")

    // no-op if the primary name is already set to this alias record
    if (newPrimaryName.nameType == PRIMARY) return

    personService.processUpdateAndPublish(newPrimaryName.person!!) { personEntity ->
      personEntity.getPrimaryName().nameType = ALIAS
      newPrimaryName.nameType = PRIMARY
    }
  }
}
