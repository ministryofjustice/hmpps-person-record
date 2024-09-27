package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class PersonBlockingRulesRepository {
  @PersistenceContext
  private val entityManager: EntityManager? = null

  fun findMatchCandidates(person: Person, sourceSystem: String?, hasPersonKey: Boolean): List<PersonEntity> {
    val query = entityManager!!.createNativeQuery(findMatchCandidatesGenerateSQL(person, sourceSystem, hasPersonKey), PersonEntity::class.java)
    return query.resultList as List<PersonEntity>
  }

  fun findMatchCandidatesGenerateSQL(person: Person, sourceSystem: String?, hasPersonKey: Boolean): String {
    var sql: String = ""
    var sourceSystemCondition = when {
      sourceSystem.isNullOrEmpty() -> "" else -> "AND pe1_0.source_system = '$sourceSystem'"
    }
    var personKeyCondition = when {
      hasPersonKey -> "AND pe1_0.fk_person_key_id IS NOT NULL" else -> ""
    }

    person.getIdentifiersForMatching().forEach {
      sql += """ 
      $SELECT_EXPRESSION
      INNER JOIN personrecordservice.reference r1_0
      ON pe1_0.id = r1_0.fk_person_id
      WHERE
      r1_0.identifier_type = '${it.identifierType.name}'
      AND r1_0.identifier_value = '${it.identifierValue}'
      AND pe1_0.merged_to IS NULL
      $personKeyCondition
      $sourceSystemCondition
      UNION
      """
    }

    sql += getPostcodeSQL(person, personKeyCondition, sourceSystemCondition)

    return sql + getDateOfBirthSQL(person, personKeyCondition, sourceSystemCondition)
  }

  private fun getDateOfBirthSQL(
    person: Person,
    personKeyCondition: String,
    sourceSystemCondition: String,
  ): String = """
        $SELECT_EXPRESSION
        WHERE
        personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex('${person.firstName}')
        AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex('${person.lastName}')
        AND date_part('year', pe1_0.date_of_birth) = ${person.dateOfBirth?.year}
        AND date_part('month', pe1_0.date_of_birth) = ${person.dateOfBirth?.monthValue}
        AND pe1_0.merged_to IS NULL
        $personKeyCondition
        $sourceSystemCondition
  
        UNION
  
        $SELECT_EXPRESSION
        WHERE
        personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex('${person.firstName}')
        AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex('${person.lastName}')
        AND date_part('year', pe1_0.date_of_birth) = ${person.dateOfBirth?.year}
        AND date_part('day', pe1_0.date_of_birth) = ${person.dateOfBirth?.dayOfMonth}
        AND pe1_0.merged_to IS NULL
        $personKeyCondition
        $sourceSystemCondition
  
        UNION
  
        $SELECT_EXPRESSION
        WHERE
        personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex('${person.firstName}')
        AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex('${person.lastName}')
        AND date_part('month', pe1_0.date_of_birth) = ${person.dateOfBirth?.monthValue}
        AND date_part('day', pe1_0.date_of_birth) = ${person.dateOfBirth?.dayOfMonth}
        AND pe1_0.merged_to IS NULL
        $personKeyCondition
        $sourceSystemCondition
        ;
  
    """

  private fun getPostcodeSQL(
    person: Person,
    personKeyCondition: String,
    sourceSystemCondition: String,
  ): String {
    var sql1 = ""
    person.addresses.mapNotNull { it.postcode }.forEach {
      sql1 += """
        $SELECT_EXPRESSION
        INNER JOIN personrecordservice.address a1_0
        ON pe1_0.id = a1_0.fk_person_id
        WHERE
        personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex('${person.firstName}')
        AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex('${person.lastName}')
        AND LEFT(a1_0.postcode, $POSTCODE_MATCH_SIZE) = '${it.take(POSTCODE_MATCH_SIZE)}'
        AND pe1_0.merged_to IS NULL
        $personKeyCondition
        $sourceSystemCondition
        UNION
      """.trimIndent()
    }
    return sql1
  }

  companion object {
    const val POSTCODE_MATCH_SIZE = 3
    const val SELECT_EXPRESSION = """
    SELECT
    pe1_0.id,pe1_0.birth_country,pe1_0.birth_place,pe1_0.crn,pe1_0.currently_managed,pe1_0.date_of_birth,pe1_0.defendant_id,pe1_0.ethnicity,pe1_0.first_name,pe1_0.last_name,pe1_0.master_defendant_id,pe1_0.merged_to,pe1_0.middle_names,pe1_0.nationality,pe1_0.fk_person_key_id,pe1_0.prison_number,pe1_0.religion,pe1_0.self_match_score,pe1_0.sex,pe1_0.sexual_orientation,pe1_0.source_system,pe1_0.title,pe1_0.version
    FROM
    personrecordservice.person pe1_0 
    """
  }
}
