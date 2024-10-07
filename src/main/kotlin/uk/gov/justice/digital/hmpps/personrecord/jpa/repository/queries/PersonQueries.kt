package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries

import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC

object PersonQueries {

  const val SEARCH_VERSION = "1.5"
  val SEARCH_IDENTIFIERS = listOf(PNC, CRO, NATIONAL_INSURANCE_NUMBER, DRIVER_LICENSE_NUMBER)

  fun findCandidatesWithUuid(person: Person): PersonQuery = PersonQuery(
    queryName = PersonQueryType.FIND_CANDIDATES_WITH_UUID,
    query = generateFindCandidatesSQL(
      BlockingRules(globalConditions = BlockingRules.hasPersonKey()),
      person,
    ),
  )

  fun findCandidatesBySourceSystem(person: Person): PersonQuery = PersonQuery(
    queryName = PersonQueryType.FIND_CANDIDATES_BY_SOURCE_SYSTEM,
    query = generateFindCandidatesSQL(
      BlockingRules(globalConditions = BlockingRules.exactMatchSourceSystem(person.sourceSystemType)),
      person,
    ),
  )

  private fun generateFindCandidatesSQL(blockingRules: BlockingRules, person: Person): String {
    val rules: MutableList<String> = mutableListOf()
    rules.addAll(
      person.getIdentifiersForMatching().map {
        blockingRules.exactMatchOnIdentifier(it.identifierType, it.identifierValue)
      },
    )
    rules.addAll(
      person.addresses.mapNotNull { it.postcode }.map {
        blockingRules.matchFirstPartPostcode(it)
      },
    )
    rules.addAll(twoDatePartMatch(blockingRules, person))
    return blockingRules.union(rules)
  }

  private fun twoDatePartMatch(blockingRules: BlockingRules, person: Person): List<String> {
    return listOf(
      blockingRules.yearAndDayMatch(person.dateOfBirth),
      blockingRules.monthAndDayMatch(person.dateOfBirth),
      blockingRules.yearAndMonthMatch(person.dateOfBirth),
    )
  }
}
