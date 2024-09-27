package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS

@Repository
interface PersonRepository : JpaSpecificationExecutor<PersonEntity>, JpaRepository<PersonEntity, Long> {

  fun findByDefendantId(defendantId: String): PersonEntity?

  fun findByCrn(crn: String): PersonEntity?

  fun findByPrisonNumberAndSourceSystem(prisonNumber: String, sourceSystem: SourceSystemType? = NOMIS): PersonEntity?

  @Query(
    nativeQuery = true,
    value = """
      SELECT
      pe1_0.id,pe1_0.birth_country,pe1_0.birth_place,pe1_0.crn,pe1_0.currently_managed,pe1_0.date_of_birth,pe1_0.defendant_id,pe1_0.ethnicity,pe1_0.first_name,pe1_0.last_name,pe1_0.master_defendant_id,pe1_0.merged_to,pe1_0.middle_names,pe1_0.nationality,pe1_0.fk_person_key_id,pe1_0.prison_number,pe1_0.religion,pe1_0.self_match_score,pe1_0.sex,pe1_0.sexual_orientation,pe1_0.source_system,pe1_0.title,pe1_0.version
      FROM
      personrecordservice.person pe1_0
      INNER JOIN personrecordservice.reference r1_0
      ON pe1_0.id = r1_0.fk_person_id
      WHERE
      r1_0.identifier_type = :#{#person.identifiersForMatching()[0].identifierType.name}
      AND r1_0.identifier_value = :#{#person.identifiersForMatching()[0].identifierValue}
      AND pe1_0.merged_to IS NULL
      AND pe1_0.fk_person_key_id IS NOT NULL

      UNION

      SELECT
      pe1_0.id,pe1_0.birth_country,pe1_0.birth_place,pe1_0.crn,pe1_0.currently_managed,pe1_0.date_of_birth,pe1_0.defendant_id,pe1_0.ethnicity,pe1_0.first_name,pe1_0.last_name,pe1_0.master_defendant_id,pe1_0.merged_to,pe1_0.middle_names,pe1_0.nationality,pe1_0.fk_person_key_id,pe1_0.prison_number,pe1_0.religion,pe1_0.self_match_score,pe1_0.sex,pe1_0.sexual_orientation,pe1_0.source_system,pe1_0.title,pe1_0.version
      FROM
      personrecordservice.person pe1_0
      WHERE
      personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex(:#{#person.firstName})
      AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex(:#{#person.lastName})
      AND date_part('year', pe1_0.date_of_birth) = :#{#person.dateOfBirth.year}
      AND date_part('month', pe1_0.date_of_birth) = :#{#person.dateOfBirth.getMonthValue}
      AND pe1_0.merged_to IS NULL
      AND pe1_0.fk_person_key_id IS NOT NULL

      UNION

      SELECT
      pe1_0.id,pe1_0.birth_country,pe1_0.birth_place,pe1_0.crn,pe1_0.currently_managed,pe1_0.date_of_birth,pe1_0.defendant_id,pe1_0.ethnicity,pe1_0.first_name,pe1_0.last_name,pe1_0.master_defendant_id,pe1_0.merged_to,pe1_0.middle_names,pe1_0.nationality,pe1_0.fk_person_key_id,pe1_0.prison_number,pe1_0.religion,pe1_0.self_match_score,pe1_0.sex,pe1_0.sexual_orientation,pe1_0.source_system,pe1_0.title,pe1_0.version
      FROM
      personrecordservice.person pe1_0
      WHERE
      personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex(:#{#person.firstName})
      AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex(:#{#person.lastName})
      AND date_part('year', pe1_0.date_of_birth) = :#{#person.dateOfBirth.year}
      AND date_part('day', pe1_0.date_of_birth) = :#{#person.dateOfBirth.dayOfMonth}
      AND pe1_0.merged_to IS NULL
      AND pe1_0.fk_person_key_id IS NOT NULL

      UNION

      SELECT
      pe1_0.id,pe1_0.birth_country,pe1_0.birth_place,pe1_0.crn,pe1_0.currently_managed,pe1_0.date_of_birth,pe1_0.defendant_id,pe1_0.ethnicity,pe1_0.first_name,pe1_0.last_name,pe1_0.master_defendant_id,pe1_0.merged_to,pe1_0.middle_names,pe1_0.nationality,pe1_0.fk_person_key_id,pe1_0.prison_number,pe1_0.religion,pe1_0.self_match_score,pe1_0.sex,pe1_0.sexual_orientation,pe1_0.source_system,pe1_0.title,pe1_0.version
      FROM
      personrecordservice.person pe1_0
      WHERE
      personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex(:#{#person.firstName})
      AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex(:#{#person.lastName})
      AND date_part('month', pe1_0.date_of_birth) = :#{#person.dateOfBirth.getMonthValue}
      AND date_part('day', pe1_0.date_of_birth) = :#{#person.dateOfBirth.dayOfMonth}
      AND pe1_0.merged_to IS NULL
      AND pe1_0.fk_person_key_id IS NOT NULL

      UNION

      SELECT
     pe1_0.id,pe1_0.birth_country,pe1_0.birth_place,pe1_0.crn,pe1_0.currently_managed,pe1_0.date_of_birth,pe1_0.defendant_id,pe1_0.ethnicity,pe1_0.first_name,pe1_0.last_name,pe1_0.master_defendant_id,pe1_0.merged_to,pe1_0.middle_names,pe1_0.nationality,pe1_0.fk_person_key_id,pe1_0.prison_number,pe1_0.religion,pe1_0.self_match_score,pe1_0.sex,pe1_0.sexual_orientation,pe1_0.source_system,pe1_0.title,pe1_0.version
      FROM
      personrecordservice.person pe1_0
      INNER JOIN personrecordservice.address a1_0
      ON pe1_0.id = a1_0.fk_person_id
      WHERE
      personrecordservice.soundex(pe1_0.first_name) = personrecordservice.soundex(:#{#person.firstName})
      AND personrecordservice.soundex(pe1_0.last_name) = personrecordservice.soundex(:#{#person.lastName})
      AND LEFT(a1_0.postcode, 3) = '{person.addresses[0].postcode[:3]}'
      AND pe1_0.merged_to IS NULL
      AND pe1_0.fk_person_key_id IS NOT NULL

      OFFSET 0 ROWS FETCH FIRST 1 ROWS ONLY;

  """,
  )
  fun findMatchCandidates(@Param("person") person: Person): List<PersonEntity>
}
