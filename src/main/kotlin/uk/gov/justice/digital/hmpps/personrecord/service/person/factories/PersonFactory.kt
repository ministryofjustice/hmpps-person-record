package uk.gov.justice.digital.hmpps.personrecord.service.person.factories

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EthnicityCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference.NationalityFactory
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference.PseudonymFactory

@Component
class PersonFactory(
  private val personRepository: PersonRepository,
  private val pseudonymFactory: PseudonymFactory,
  private val nationalityFactory: NationalityFactory,
  private val titleCodeRepository: TitleCodeRepository,
  private val ethnicityCodeRepository: EthnicityCodeRepository,

) {

  fun create(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person)
    personEntity.buildChildEntities(person)
    return personRepository.save(personEntity)
  }

  fun update(person: Person, personEntity: PersonEntity) {
    personEntity.update(person)
    personEntity.buildChildEntities(person)
    personRepository.save(personEntity)
  }

  private fun PersonEntity.buildChildEntities(person: Person) {
    this.attachPseudonyms(pseudonymFactory.buildPseudonyms(person))
    this.attachNationalities(nationalityFactory.buildNationalities(person))
  }

  private fun PersonEntity.attachPseudonyms(pseudonyms: List<PseudonymEntity>) {
    this.pseudonyms.clear()
    pseudonyms.forEach { pseudonymEntity -> pseudonymEntity.person = this }
    this.pseudonyms.addAll(pseudonyms)
  }

  private fun PersonEntity.attachNationalities(nationalities: List<NationalityEntity>) {
    this.nationalities.clear()
    nationalities.forEach { nationalityEntity -> nationalityEntity.person = this }
    this.nationalities.addAll(nationalities)
  }
  private fun lookupEthnicityCode(ethnicityCode: EthnicityCode?): EthnicityCodeEntity? = ethnicityCode?.let { ethnicityCodeRepository.findByCode(it.name) }
}
