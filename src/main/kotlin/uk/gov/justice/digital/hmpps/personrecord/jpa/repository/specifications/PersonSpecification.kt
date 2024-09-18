package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications

import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType.LEFT
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import java.time.LocalDate

object PersonSpecification {

  const val SEARCH_VERSION = "2"

  val SEARCH_IDENTIFIERS = listOf(PNC, CRO, NATIONAL_INSURANCE_NUMBER, DRIVER_LICENSE_NUMBER)

  const val FIRST_NAME = "firstName"
  const val LAST_NAME = "lastName"
  const val DOB = "dateOfBirth"
  const val SOURCE_SYSTEM = "sourceSystem"

  private const val IDENTIFIER_TYPE = "identifierType"
  private const val IDENTIFIER_VALUE = "identifierValue"
  private const val PERSON_KEY = "personKey"
  private const val POSTCODE = "postcode"
  private const val MERGED_TO = "mergedTo"

  fun exactMatch(input: String?, field: String): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.takeIf { it.isNotBlank() }?.let {
        criteriaBuilder.equal(root.get<String>(field), it)
      }
    }
  }

  fun exactMatchReferences(references: List<Reference>): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      references.takeIf { it.isNotEmpty() }?.let {
        val referencesJoin: Join<PersonEntity, ReferenceEntity> = root.join("references", LEFT)
        val referencePredicates: Array<Predicate> = references.map { reference ->
          criteriaBuilder.and(
            criteriaBuilder.equal(referencesJoin.get<IdentifierType>(IDENTIFIER_TYPE), reference.identifierType),
            criteriaBuilder.equal(referencesJoin.get<String>(IDENTIFIER_VALUE), reference.identifierValue),
          )
        }.toTypedArray()
        criteriaBuilder.or(*referencePredicates)
      }
    }
  }

  fun soundex(input: String?, field: String): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.let {
        criteriaBuilder.equal(
          criteriaBuilder.function("SOUNDEX", String::class.java, root.get<String>(field)),
          criteriaBuilder.function("SOUNDEX", String::class.java, criteriaBuilder.literal(it)),
        )
      } ?: criteriaBuilder.disjunction()
    }
  }

  fun exactMatchPostcodePrefix(postcodes: Set<String>, prefixLength: Int = 3): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      postcodes.takeIf { it.isNotEmpty() }?.let {
        val addressJoin: Join<PersonEntity, AddressEntity> = root.join("addresses", LEFT)
        val postcodePredicates: Array<Predicate> = postcodes.map {
          criteriaBuilder.equal(criteriaBuilder.function("LEFT", String::class.java, addressJoin.get<String>(POSTCODE), criteriaBuilder.literal(prefixLength)), it.take(prefixLength))
        }.toTypedArray()
        criteriaBuilder.or(*postcodePredicates)
      } ?: criteriaBuilder.disjunction()
    }
  }

  fun matchDateParts(input: LocalDate?, field: String): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.let {
        val yearMatch: Predicate = criteriaBuilder.equal(
          criteriaBuilder.function(
            "date_part",
            Int::class.java, criteriaBuilder.literal("YEAR"), root.get<LocalDate>(field),
          ),
          input.year,
        )
        val monthMatch: Predicate = criteriaBuilder.equal(
          criteriaBuilder.function(
            "date_part",
            Int::class.java, criteriaBuilder.literal("MONTH"), root.get<LocalDate>(field),
          ),
          input.monthValue,
        )
        val dayMatch: Predicate = criteriaBuilder.equal(
          criteriaBuilder.function(
            "date_part",
            Int::class.java, criteriaBuilder.literal("DAY"), root.get<LocalDate>(field),
          ),
          input.dayOfMonth,
        )
        criteriaBuilder.or(
          criteriaBuilder.and(yearMatch, monthMatch),
          criteriaBuilder.and(yearMatch, dayMatch),
          criteriaBuilder.and(monthMatch, dayMatch),
        )
      } ?: criteriaBuilder.disjunction()
    }
  }

  fun hasPersonKey(): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      criteriaBuilder.isNotNull(root.get<Long>(PERSON_KEY))
    }
  }

  fun isNotMerged(): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      criteriaBuilder.isNull(root.get<Long>(MERGED_TO))
    }
  }
}
