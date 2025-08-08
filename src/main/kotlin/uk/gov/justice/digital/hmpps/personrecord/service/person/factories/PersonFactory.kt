package uk.gov.justice.digital.hmpps.personrecord.service.person.factories

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.TitleCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EthnicityCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.TitleCodeRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode

@Component
class PersonFactory(
  private val personRepository: PersonRepository,
  private val titleCodeRepository: TitleCodeRepository,
  private val ethnicityCodeRepository: EthnicityCodeRepository,

) {

  fun create(person: Person): PersonEntity {
    val personEntity = PersonEntity.new(person, lookupEthnicityCode(person.ethnicityCode))
    personEntity.buildPseudonyms(person)
    return personRepository.save(personEntity)
  }

  fun update(person: Person, personEntity: PersonEntity) {
    personEntity.update(person)
    personEntity.buildPseudonyms(person)
    personRepository.save(personEntity)
  }

  private fun lookupTitleCode(titleCode: TitleCode?): TitleCodeEntity? = titleCode?.let { titleCodeRepository.findByCode(it.name) }

  private fun lookupEthnicityCode(ethnicityCode: EthnicityCode?): EthnicityCodeEntity? = ethnicityCode?.let { ethnicityCodeRepository.findByCode(it.name) }

  private fun PersonEntity.buildPseudonyms(person: Person) {
    this.pseudonyms.clear()

    val primaryName = PseudonymEntity.primaryNameFrom(person, lookupTitleCode(person.titleCode))
    val aliases = person.aliases.mapNotNull { PseudonymEntity.aliasFrom(it, lookupTitleCode(it.titleCode)) }

    val pseudonyms = mutableListOf<PseudonymEntity>()
    pseudonyms.add(primaryName)
    pseudonyms.addAll(aliases)
    pseudonyms.forEach { pseudonymEntity -> pseudonymEntity.person = this }
    this.pseudonyms.addAll(pseudonyms)
  }
}
