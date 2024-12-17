package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.criteria

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.BlockingRules.Companion.POSTCODE_MATCH_SIZE
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedDateStatement
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedIdentifierStatement
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedLongStatement
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels.PreparedStringStatement
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

data class PersonSearchCriteria(
  val preparedId: PreparedLongStatement? = null,
  val preparedFirstName: PreparedStringStatement,
  val preparedLastName: PreparedStringStatement,
  val preparedDateOfBirth: PreparedDateStatement,
  val preparedIdentifiers: List<PreparedIdentifierStatement> = listOf(),
  val preparedPostcodes: List<PreparedStringStatement> = listOf(),
  val sourceSystemType: SourceSystemType,
) {
  companion object {
    private const val PERSON_ID_PARAMETER_NAME = "personId"
    private const val FIRST_NAME_PARAMETER_NAME = "firstName"
    private const val LAST_NAME_PARAMETER_NAME = "lastName"
    private const val DOB_PARAMETER_NAME = "dateOfBirth"

    private const val POSTCODE_PARAMETER_NAME_PREFIX = "postcode"
    private const val IDENTIFIER_PARAMETER_NAME_PREFIX = "identifier"

    private val SEARCH_IDENTIFIERS = listOf(PNC, CRO, NATIONAL_INSURANCE_NUMBER, DRIVER_LICENSE_NUMBER)

    fun from(person: Person): PersonSearchCriteria = PersonSearchCriteria(
      preparedFirstName = PreparedStringStatement(parameterName = FIRST_NAME_PARAMETER_NAME, value = person.firstName),
      preparedLastName = PreparedStringStatement(parameterName = LAST_NAME_PARAMETER_NAME, value = person.lastName),
      preparedDateOfBirth = PreparedDateStatement(parameterName = DOB_PARAMETER_NAME, value = person.dateOfBirth),
      preparedIdentifiers = buildPreparedIdentifiers(person.getIdentifiersForMatching(SEARCH_IDENTIFIERS)),
      preparedPostcodes = buildPreparedPostcodes(person.getPostcodesForMatching()),
      sourceSystemType = person.sourceSystem,
    )

    fun from(personEntity: PersonEntity): PersonSearchCriteria = PersonSearchCriteria(
      preparedId = PreparedLongStatement(parameterName = PERSON_ID_PARAMETER_NAME, value = personEntity.id),
      preparedFirstName = PreparedStringStatement(parameterName = FIRST_NAME_PARAMETER_NAME, value = personEntity.firstName),
      preparedLastName = PreparedStringStatement(parameterName = LAST_NAME_PARAMETER_NAME, value = personEntity.lastName),
      preparedDateOfBirth = PreparedDateStatement(parameterName = DOB_PARAMETER_NAME, value = personEntity.dateOfBirth),
      preparedIdentifiers = buildPreparedIdentifiers(
        personEntity.getIdentifiersForMatching(SEARCH_IDENTIFIERS).map {
          Reference(identifierType = it.identifierType, identifierValue = it.identifierValue)
        }.toSet(),
      ),
      preparedPostcodes = buildPreparedPostcodes(personEntity.getPostcodesForMatching()),
      sourceSystemType = personEntity.sourceSystem,
    )

    private fun buildPreparedIdentifiers(identifiers: Set<Reference>) = identifiers.mapIndexed { index, reference ->
      PreparedIdentifierStatement(parameterName = IDENTIFIER_PARAMETER_NAME_PREFIX + index, reference)
    }

    private fun buildPreparedPostcodes(postcodes: Set<String>): List<PreparedStringStatement> = postcodes.mapIndexed { index, postcode ->
      PreparedStringStatement(parameterName = POSTCODE_PARAMETER_NAME_PREFIX + index, postcode.take(POSTCODE_MATCH_SIZE))
    }
  }
}
