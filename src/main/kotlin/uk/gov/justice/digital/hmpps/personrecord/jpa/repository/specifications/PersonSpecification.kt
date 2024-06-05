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
}
