package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

object PersonSpecification {

  const val PNC = "pnc"
  const val CRO = "cro"
  const val NI = "nationalInsuranceNumber"
  const val DRIVER_LICENSE_NUMBER = "driverLicenseNumber"
  const val FIRST_NAME = "firstName"
  const val LAST_NAME = "lastName"
  const val DOB = "dateOfBirth"

  private const val POSTCODE = "postcode"
  private const val DATE_FORMAT = "YYYY-MM-DD"

  fun exactMatch(input: String?, field: String): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.let {
        criteriaBuilder.equal(root.get<String>(field), it)
      }
    }
  }

  fun soundex(input: String?, field: String): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.let {
        criteriaBuilder.equal(
          criteriaBuilder.function("SOUNDEX", String::class.java, root.get<String>(field)),
          criteriaBuilder.function("SOUNDEX", String::class.java, criteriaBuilder.literal(input)),
        )
      }
    }
  }

  fun levenshteinPostcode(input: String?, limit: Int = 2): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      input?.let {
        val addressJoin = root.join<PersonEntity, AddressEntity>("addresses", JoinType.INNER)
        criteriaBuilder.le(
          criteriaBuilder.function("levenshtein", Integer::class.java, criteriaBuilder.literal(it), addressJoin.get<String>(POSTCODE)),
          limit,
        )
      }
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
          criteriaBuilder.function("levenshtein", Integer::class.java, criteriaBuilder.literal(it.toString()), dbDateAsString),
          limit,
        )
      }
    }
  }

  fun <T> combineSpecificationsWithOr(specifications: List<Specification<T>>): Specification<T>? {
    if (specifications.isEmpty()) return null
    var combinedSpec: Specification<T> = specifications[0]
    specifications.forEach { specification ->
      combinedSpec = combinedSpec.or(specification)
    }
    return combinedSpec
  }
}
