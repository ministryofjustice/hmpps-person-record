package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries

enum class PersonQueryType {
  FIND_CANDIDATES_WITH_UUID,
  FIND_CANDIDATES_BY_SOURCE_SYSTEM,
}

data class PersonQuery(
  val queryName: PersonQueryType,
  val query: String,
)
