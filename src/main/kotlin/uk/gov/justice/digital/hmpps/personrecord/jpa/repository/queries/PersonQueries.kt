package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries

import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria.PersonSearchCriteria

object PersonQueries {

  const val SEARCH_VERSION = "1.5"
  private const val BLOCKING_RULES_SEPARATOR = " "

  fun findCandidatesWithUuid(searchCriteria: PersonSearchCriteria): PersonQuery = PersonQuery(
    queryName = PersonQueryType.FIND_CANDIDATES_WITH_UUID,
    query = generateFindCandidatesSQL(
      BlockingRules(
        globalConditions = combineBlockingRules(
          BlockingRules.hasPersonKey(),
          BlockingRules.hasNoMergeLink(),
          BlockingRules.notSelf(personIdParameterName = searchCriteria.preparedId?.parameterName),
        ),
      ),
      searchCriteria,
    ),
  )

  private fun generateFindCandidatesSQL(blockingRules: BlockingRules, searchCriteria: PersonSearchCriteria): String {
    val rules: MutableList<String> = mutableListOf()
    rules.addAll(
      searchCriteria.preparedIdentifiers.map {
        blockingRules.exactMatchOnIdentifier(it.reference.identifierType, it.parameterName)
      },
    )
    rules.addAll(
      searchCriteria.preparedPostcodes.map {
        blockingRules.matchFirstPartPostcode(it.parameterName)
      },
    )
    rules.addAll(twoDatePartMatch(blockingRules, searchCriteria))
    return blockingRules.union(rules)
  }

  private fun twoDatePartMatch(blockingRules: BlockingRules, searchCriteria: PersonSearchCriteria): List<String> = listOf(
    blockingRules.yearAndDayMatch(
      yearParameterName = searchCriteria.preparedDateOfBirth.year.parameterName,
      dayParameterName = searchCriteria.preparedDateOfBirth.day.parameterName,
    ),
    blockingRules.monthAndDayMatch(
      monthParameterName = searchCriteria.preparedDateOfBirth.month.parameterName,
      dayParameterName = searchCriteria.preparedDateOfBirth.day.parameterName,
    ),
    blockingRules.yearAndMonthMatch(
      yearParameterName = searchCriteria.preparedDateOfBirth.year.parameterName,
      monthParameterName = searchCriteria.preparedDateOfBirth.month.parameterName,
    ),
  )

  private fun combineBlockingRules(vararg rules: String): String = rules.toList().joinToString(BLOCKING_RULES_SEPARATOR)
}
