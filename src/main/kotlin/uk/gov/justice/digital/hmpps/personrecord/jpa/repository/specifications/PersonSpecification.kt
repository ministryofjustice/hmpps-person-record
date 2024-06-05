package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.specifications

import org.springframework.data.jpa.domain.Specification
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

object PersonSpecification {

  fun pncEquals(pnc: String?): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      pnc?.let {
        criteriaBuilder.equal(root.get<String>("pnc"), it)
      }
    }
  }

  fun croEquals(cro: String?): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      cro?.let {
        criteriaBuilder.equal(root.get<String>("cro"), it)
      }
    }
  }

  fun driverLicenseEquals(driverLicenseNumber: String?): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      driverLicenseNumber?.let {
        criteriaBuilder.equal(root.get<String>("driverLicenseNumber"), it)
      }
    }
  }

  fun nationalInsuranceNumberEquals(nationalInsuranceNumber: String?): Specification<PersonEntity> {
    return Specification { root, _, criteriaBuilder ->
      nationalInsuranceNumber?.let {
        criteriaBuilder.equal(root.get<String>("nationalInsuranceNumber"), it)
      }
    }
  }
}
