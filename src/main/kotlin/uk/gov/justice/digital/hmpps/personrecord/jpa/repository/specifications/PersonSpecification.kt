package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications

import jakarta.persistence.criteria.Join
import jakarta.persistence.criteria.JoinType.INNER
import jakarta.persistence.criteria.JoinType.LEFT
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonIdentifierEntity
import java.time.LocalDate

object PersonSpecification {

  const val SEARCH_VERSION = "1.4"

  const val PNC = "pnc"
  const val CRO = "cro"
  const val NI = "nationalInsuranceNumber"
  const val DRIVER_LICENSE_NUMBER = "driverLicenseNumber"
  const val FIRST_NAME = "firstName"
  const val LAST_NAME = "lastName"
  const val DOB = "dateOfBirth"
  const val SOURCE_SYSTEM = "sourceSystem"

  private const val POSTCODE = "postcode"
  private const val DATE_FORMAT = "YYYY-MM-DD"

  fun exactMatch(input: String?, field: String): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.takeIf { it.isNotBlank() }?.let {
        criteriaBuilder.equal(root.get<String>(field), it)
      }
    }
  }

  fun soundex(input: String?, field: String): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      criteriaBuilder.and(
        criteriaBuilder.isNotNull(criteriaBuilder.literal(input)),
        criteriaBuilder.equal(
          criteriaBuilder.function("SOUNDEX", String::class.java, root.get<String>(field)),
          criteriaBuilder.function("SOUNDEX", String::class.java, criteriaBuilder.literal(input)),
        ),
      )
    }
  }

  fun levenshteinPostcodes(postcodes: Set<String>, limit: Int = 1): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      postcodes.takeIf { it.isNotEmpty() }?.let {
        val addressJoin: Join<PersonEntity, AddressEntity> = root.join("addresses", INNER)
        val postcodePredicates: Array<Predicate> = postcodes.map {
          criteriaBuilder.le(
            criteriaBuilder.function("levenshtein", Integer::class.java, criteriaBuilder.literal(it), addressJoin.get<String>(POSTCODE)),
            limit,
          )
        }.toTypedArray()
        criteriaBuilder.or(*postcodePredicates)
      } ?: criteriaBuilder.disjunction()
    }
  }

  fun levenshteinDate(input: LocalDate?, field: String, limit: Int = 2): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.let {
        val dbDateAsString = criteriaBuilder.function(
          "TO_CHAR",
          String::class.java,
          root.get<LocalDate>(field),
          criteriaBuilder.literal(DATE_FORMAT),
        )
        criteriaBuilder.le(
          criteriaBuilder.function("levenshtein", Integer::class.java, criteriaBuilder.literal(input.toString()), dbDateAsString),
          limit,
        )
      } ?: criteriaBuilder.disjunction()
    }
  }

  fun hasPersonIdentifier(): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      val personIdentifierJoin: Join<PersonEntity, PersonIdentifierEntity> = root.join("personIdentifier", LEFT)
      criteriaBuilder.isNotNull(personIdentifierJoin)
    }
  }
}
