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
    val addresses = canonicalRecord.addresses.filter { it.status.code != AddressStatusCode.P.name }
    return canonicalRecord.copy(addresses = addresses)
  }
}
