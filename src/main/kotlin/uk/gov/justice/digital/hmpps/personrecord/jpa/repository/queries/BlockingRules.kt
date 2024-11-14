package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries

import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDate

class BlockingRules(
  private val globalConditions: String = "",
) {

  fun exactMatchOnIdentifier(identifierType: IdentifierType, identifierValue: String?): String = """ 
      $SELECT_EXPRESSION
      INNER JOIN personrecordservice.reference r1_0
      ON pe1_0.id = r1_0.fk_person_id
      WHERE
        r1_0.identifier_type = '${identifierType.name}'
        AND r1_0.identifier_value = '$identifierValue'
      $globalConditions
  """.trimIndent()

  fun matchFirstPartPostcode(postcode: String): String = """
      $SELECT_EXPRESSION
      INNER JOIN personrecordservice.address a1_0
      ON pe1_0.id = a1_0.fk_person_id
        $SOUNDEX_EXPRESSION
        AND LEFT(a1_0.postcode, $POSTCODE_MATCH_SIZE) = '${postcode.take(POSTCODE_MATCH_SIZE)}'
      $globalConditions
  """.trimIndent()

  fun yearAndMonthMatch(dateOfBirth: LocalDate?): String = """
      $SELECT_EXPRESSION
      $SOUNDEX_EXPRESSION
        AND date_part('year', pe1_0.date_of_birth) = ${dateOfBirth?.year}
        AND date_part('month', pe1_0.date_of_birth) = ${dateOfBirth?.monthValue}
      $globalConditions
  """.trimIndent()

  fun yearAndDayMatch(dateOfBirth: LocalDate?): String = """
      $SELECT_EXPRESSION
      $SOUNDEX_EXPRESSION
        AND date_part('year', pe1_0.date_of_birth) = ${dateOfBirth?.year}
        AND date_part('day', pe1_0.date_of_birth) = ${dateOfBirth?.dayOfMonth}
      $globalConditions
  """.trimIndent()

  fun monthAndDayMatch(dateOfBirth: LocalDate?): String = """
      $SELECT_EXPRESSION
      $SOUNDEX_EXPRESSION
        AND date_part('month', pe1_0.date_of_birth) = ${dateOfBirth?.monthValue}
        AND date_part('day', pe1_0.date_of_birth) = ${dateOfBirth?.dayOfMonth}
      $globalConditions
  """.trimIndent()

  fun union(rules: List<String>): String {
    return rules.joinToString(UNION)
  }

  companion object {
    const val POSTCODE_MATCH_SIZE = 3
    const val SELECT_EXPRESSION = """
      SELECT
        pe1_0.id,pe1_0.birth_country,pe1_0.birth_place,pe1_0.crn,pe1_0.currently_managed,pe1_0.date_of_birth,pe1_0.defendant_id,pe1_0.ethnicity,pe1_0.first_name,pe1_0.last_name,pe1_0.master_defendant_id,pe1_0.merged_to,pe1_0.middle_names,pe1_0.nationality,pe1_0.fk_person_key_id,pe1_0.prison_number,pe1_0.religion,pe1_0.self_match_score,pe1_0.sex,pe1_0.sexual_orientation,pe1_0.source_system,pe1_0.title,pe1_0.version
      FROM
        personrecordservice.person pe1_0 
      """
    const val SOUNDEX_EXPRESSION = """
      WHERE
        personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex(:firstName)
        AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex(:lastName)
      """
    const val UNION = """
      UNION
    """

    fun exactMatchSourceSystem(sourceSystemType: SourceSystemType): String = """
      AND pe1_0.source_system = '${sourceSystemType.name}'
    """.trimIndent()

    fun hasNoMergeLink(): String = """
      AND pe1_0.merged_to IS NULL
    """.trimIndent()

    fun hasPersonKey(): String = """
      AND pe1_0.fk_person_key_id IS NOT NULL
    """.trimIndent()

    fun notSelf(id: Long?): String = id?.let {
      """
       AND pe1_0.id != '$it'
      """.trimIndent()
    } ?: ""
  }
}
