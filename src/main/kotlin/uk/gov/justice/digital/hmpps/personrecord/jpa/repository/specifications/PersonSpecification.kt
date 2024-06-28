package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications

import jakarta.persistence.criteria.JoinType
import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate

object PersonSpecification {

  const val SEARCH_VERSION = "1.3"

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

  fun levenshteinPostcode(input: String?, limit: Int = 2): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      val addressJoin = root.join<PersonEntity, AddressEntity>("addresses", JoinType.INNER)
      criteriaBuilder.le(
        criteriaBuilder.function("levenshtein", Integer::class.java, criteriaBuilder.literal(input), addressJoin.get<String>(POSTCODE)),
        limit,
      )
    }
  }

  fun levenshteinDate(input: LocalDate?, field: String, limit: Int = 2): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      val dbDateAsString = criteriaBuilder.function(
        "TO_CHAR",
        String::class.java,
        root.get<LocalDate>(field),
        criteriaBuilder.literal(DATE_FORMAT),
      )
      criteriaBuilder.and(
        criteriaBuilder.isNotNull(criteriaBuilder.literal(input)),
        criteriaBuilder.le(
          criteriaBuilder.function("levenshtein", Integer::class.java, criteriaBuilder.literal(input.toString()), dbDateAsString),
          limit,
        ),
      )
    }
  }

  fun <T> combineSpecificationsWithOr(specifications: List<Specification<T>>): Specification<T>? {
    if (specifications.isEmpty()) return null
    var combinedSpec: Specification<T> = specifications[0]
    specifications.takeLast(specifications.size - 1).forEach { specification ->
      combinedSpec = combinedSpec.or(specification)
    }
    return combinedSpec
  }
}
