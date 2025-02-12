package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries

enum class PersonQueryType {
  FIND_CANDIDATES_WITH_UUID,
}

data class PersonQuery(
  val queryName: PersonQueryType,
  val query: String,
)
