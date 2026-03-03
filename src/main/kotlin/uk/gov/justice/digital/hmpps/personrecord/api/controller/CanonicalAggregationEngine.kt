package uk.gov.justice.digital.hmpps.personrecord.api.controller

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.canonical.CanonicalRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

@Component
class CanonicalAggregationEngine(
  private val personRepository: PersonRepository,
) {

  fun get(personKeyEntity: PersonKeyEntity): CanonicalRecord {
    val canonicalRecord = CanonicalRecord.from(personKeyEntity)
    return canonicalRecord
  }
}
