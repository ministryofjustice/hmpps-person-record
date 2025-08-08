package uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.NationalityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.NationalityCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Nationality
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode

@Component
class NationalityFactory(
  private val nationalityCodeRepository: NationalityCodeRepository,
) {
  fun buildNationalities(person: Person): List<NationalityEntity> = person.nationalities.mapNotNull { it.build() }

  private fun Nationality.build(): NationalityEntity? = NationalityEntity.from(this, lookupNationalityCode(this.code))

  private fun lookupNationalityCode(nationalityCode: NationalityCode?): NationalityCodeEntity? = nationalityCode?.let { nationalityCodeRepository.findByCode(it.name) }
}
