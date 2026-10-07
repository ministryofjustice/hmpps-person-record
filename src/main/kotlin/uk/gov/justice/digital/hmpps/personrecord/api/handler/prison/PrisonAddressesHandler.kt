package uk.gov.justice.digital.hmpps.personrecord.api.handler.prison

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalAddress
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class PrisonAddressesHandler(
  private val personRepository: PersonRepository,
) {
  @Transactional(readOnly = true)
  fun get(prisonNumber: String): List<CanonicalAddress> = findPerson(prisonNumber).addresses
    // same sort order as was in Prison API previously
    .sortedWith(compareByDescending(nullsFirst()) { it.startDate })
    .map { CanonicalAddress.from(it) }

  private fun findPerson(prisonNumber: String): PersonEntity = personRepository.findByPrisonNumber(prisonNumber)
    ?: throw ResourceNotFoundException("Person with $prisonNumber not found")
}
