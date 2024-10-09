package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries

import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria

object PersonQueries {

  const val SEARCH_VERSION = "1.5"

  fun findCandidatesWithUuid(searchCriteria: PersonSearchCriteria): PersonQuery = PersonQuery(
    queryName = PersonQueryType.FIND_CANDIDATES_WITH_UUID,
    query = generateFindCandidatesSQL(
      BlockingRules(globalConditions = BlockingRules.hasPersonKey()),
      searchCriteria,
    ),
  )

  fun findCandidatesBySourceSystem(searchCriteria: PersonSearchCriteria): PersonQuery = PersonQuery(
    queryName = PersonQueryType.FIND_CANDIDATES_BY_SOURCE_SYSTEM,
    query = generateFindCandidatesSQL(
      BlockingRules(globalConditions = BlockingRules.exactMatchSourceSystem(searchCriteria.sourceSystemType)),
      searchCriteria,
    ),
  )

  private fun generateFindCandidatesSQL(blockingRules: BlockingRules, searchCriteria: PersonSearchCriteria): String {
    val rules: MutableList<String> = mutableListOf()
    rules.addAll(
      searchCriteria.identifiers.map {
        blockingRules.exactMatchOnIdentifier(it.identifierType, it.identifierValue)
      },
    )
    rules.addAll(
      searchCriteria.postcodes.map {
        blockingRules.matchFirstPartPostcode(it)
      },
    )
    rules.addAll(twoDatePartMatch(blockingRules, searchCriteria))
    return blockingRules.union(rules)
  }

  private fun twoDatePartMatch(blockingRules: BlockingRules, searchCriteria: PersonSearchCriteria): List<String> {
    return listOf(
      blockingRules.yearAndDayMatch(searchCriteria.dateOfBirth),
      blockingRules.monthAndDayMatch(searchCriteria.dateOfBirth),
      blockingRules.yearAndMonthMatch(searchCriteria.dateOfBirth),
    )
  }
}
