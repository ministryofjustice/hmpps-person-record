package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.queries

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

enum class PersonQueryType {
  FIND_CANDIDATES_WITH_UUID,
  FIND_CANDIDATES_BY_SOURCE_SYSTEM,
}

data class PersonQuery(
  val queryName: PersonQueryType,
  val query: Specification<PersonEntity>,
)
