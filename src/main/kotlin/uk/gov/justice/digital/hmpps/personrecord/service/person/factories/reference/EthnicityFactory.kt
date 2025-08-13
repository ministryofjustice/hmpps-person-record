package uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EthnicityCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class EthnicityFactory(
  private val ethnicityCodeRepository: EthnicityCodeRepository,
) {

  fun buildEthnicity(person: Person): EthnicityCodeEntity? = person.ethnicityCode?.let { ethnicityCodeRepository.findByCode(it.name) }
}
