package uk.gov.justice.digital.hmpps.personrecord.api.handler.court

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode

@Component
class GetCommonPlatformPersonHandler(
  private val personRepository: PersonRepository,
) {

  fun get(defendantId: String): CanonicalRecord {
    val personEntity = personRepository.findByDefendantId(defendantId) ?: throw ResourceNotFoundException(defendantId)
    val canonicalRecord = CanonicalRecord.from(personEntity)
    // We are filtering out previous address because COMMON_PLATFORM only ever send us one address (unable to determine if it's a create or update).
    // We keep the previous addresses purely for matching purposes only.
    val mainAddress = canonicalRecord.addresses.singleOrNull { it.status.code == AddressStatusCode.M.name }
    return canonicalRecord.copy(addresses = listOfNotNull(mainAddress))
  }
}
