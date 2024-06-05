package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

object PersonSpecification {

  const val PNC = "pnc"
  const val CRO = "cro"
  const val NI = "nationalInsuranceNumber"
  const val DRIVER_LICENSE_NUMBER = "driverLicenseNumber"
  const val FIRST_NAME = "firstName"

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
}
