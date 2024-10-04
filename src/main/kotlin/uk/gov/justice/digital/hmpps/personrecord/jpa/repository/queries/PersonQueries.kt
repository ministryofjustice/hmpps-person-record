package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.builders.SQLBuilder
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.SOURCE_SYSTEM
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatch
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications.PersonSpecification.exactMatchReferences
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC

private fun findCandidates(person: Person): Specification<PersonEntity> {
  val postcodes = person.addresses.mapNotNull { it.postcode }.toSet()
  val references = person.references.filter { PersonSpecification.SEARCH_IDENTIFIERS.contains(it.identifierType) && !it.identifierValue.isNullOrEmpty() }

  val soundexFirstLastName = Specification.where(
    PersonSpecification.soundex(person.firstName, PersonSpecification.FIRST_NAME)
      .and(PersonSpecification.soundex(person.lastName, PersonSpecification.LAST_NAME)),
  )
  val levenshteinDob = Specification.where(PersonSpecification.levenshteinDate(person.dateOfBirth, PersonSpecification.DOB))
  val levenshteinPostcode = Specification.where(PersonSpecification.levenshteinPostcodes(postcodes))

  return Specification.where(
    exactMatchReferences(references)
      .or(soundexFirstLastName.and(levenshteinDob.or(levenshteinPostcode)))
      .and(PersonSpecification.isNotMerged()),
  )
}

fun findCandidatesWithUuid(person: Person): PersonQuery = PersonQuery(
  queryName = PersonQueryType.FIND_CANDIDATES_WITH_UUID,
  query = findCandidates(person).and(PersonSpecification.hasPersonKey()),
)

fun findCandidatesBySourceSystem(person: Person): PersonQuery = PersonQuery(
  queryName = PersonQueryType.FIND_CANDIDATES_BY_SOURCE_SYSTEM,
  query = findCandidates(person).and(exactMatch(person.sourceSystemType.name, SOURCE_SYSTEM)),
)

object PersonQueries {

  const val SEARCH_VERSION = "1.5"

  private const val SCHEMA = "personrecordservice"
  private const val PERSON_TABLE = "$SCHEMA.person"
  private const val PERSON_TABLE_ALIAS = "p"
  private const val REFERENCE_TABLE = "$SCHEMA.reference"
  private const val REFERENCE_TABLE_ALIAS = "r"
  private const val ADDRESS_TABLE = "$SCHEMA.address"
  private const val ADDRESS_TABLE_ALIAS = "r"

  val SEARCH_IDENTIFIERS = listOf(PNC, CRO, NATIONAL_INSURANCE_NUMBER, DRIVER_LICENSE_NUMBER)

  private const val POSTCODE_MATCH_SIZE = 3
  private val SELECT_COLUMNS = arrayOf("p.id" ,"p.birth_country","p.birth_place","p.crn","p.currently_managed","p.date_of_birth","p.defendant_id","p.ethnicity","p.first_name","p.last_name","p.master_defendant_id","p.merged_to","p.middle_names","p.nationality","p.fk_person_key_id","p.prison_number","p.religion","p.self_match_score","p.sex","p.sexual_orientation","p.source_system","p.title","p.version")
  
  fun findCandidatesQuery(person: Person): String {
    val identifierSQL = identifierBlockingRule(person)
    val postcodeSQL = postcodeBlockingRule(person)
    return SQLBuilder()
      .append(identifierSQL)
      .union()
      .append(postcodeSQL)
      .build()
  }

  private fun identifierBlockingRule(person: Person, globalConditions: SQLBuilder = SQLBuilder()): SQLBuilder {
    val identifierBlockingRuleSQL = SQLBuilder()
    person.getIdentifiersForMatching().forEach {
      identifierBlockingRuleSQL.append(
        SQLBuilder()
        .select(*SELECT_COLUMNS)
        .from(PERSON_TABLE, PERSON_TABLE_ALIAS)
        .innerJoin(REFERENCE_TABLE, REFERENCE_TABLE_ALIAS)
        .on("${"$PERSON_TABLE_ALIAS.id"} = ${"$REFERENCE_TABLE_ALIAS.fk_person_id"}")
        .where("${"$REFERENCE_TABLE_ALIAS.identifier_type"} = '${it.identifierType.name}'")
          .and("${"$REFERENCE_TABLE_ALIAS.identifier_value"} = '${it.identifierValue}'")
          .append(globalConditions)
      )
    }
    return identifierBlockingRuleSQL
  }

  private fun soundexBlockingRule(): SQLBuilder {
    return SQLBuilder()
      .where("$SCHEMA.soundex($PERSON_TABLE_ALIAS.first_name) = $SCHEMA.soundex(:firstName)")
      .and("$SCHEMA.soundex($PERSON_TABLE_ALIAS.last_name) = $SCHEMA.soundex(:lastName)")
  }

  private fun postcodeBlockingRule(person: Person, globalConditions: SQLBuilder = SQLBuilder()): SQLBuilder {
    val postcodeBlockingRuleSQL = SQLBuilder()
    person.addresses.mapNotNull { it.postcode }.forEach {
      postcodeBlockingRuleSQL.append(
        SQLBuilder()
          .select(*SELECT_COLUMNS)
          .from(PERSON_TABLE, PERSON_TABLE_ALIAS)
          .innerJoin(ADDRESS_TABLE, ADDRESS_TABLE_ALIAS)
          .on("${"$PERSON_TABLE_ALIAS.id"} = ${"$ADDRESS_TABLE_ALIAS.fk_person_id"}")
          .append(soundexBlockingRule())
          .and("LEFT($ADDRESS_TABLE_ALIAS.postcode, $POSTCODE_MATCH_SIZE) = '${it.take(POSTCODE_MATCH_SIZE)}'")
          .append(globalConditions)
      )
    }
    return postcodeBlockingRuleSQL
  }
}