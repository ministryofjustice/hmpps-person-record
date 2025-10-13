package uk.gov.justice.digital.hmpps.personrecord.service.person.factories

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference.EthnicityFactory
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference.NationalityFactory
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.reference.PseudonymFactory

@Component
class PersonFactory(
  private val personRepository: PersonRepository,
  private val pseudonymFactory: PseudonymFactory,
  private val nationalityFactory: NationalityFactory,
  private val ethnicityFactory: EthnicityFactory,
) {

  fun create(person: Person): PersonChainable {
    val personEntity = PersonEntity.new(person)
    personEntity.buildChildEntities(person)
    return PersonChainable(
      personEntity = personRepository.save(personEntity),
      matchingFieldsChanged = true,
      linkOnCreate = person.behaviour.linkOnCreate,
    )
  }

  fun update(person: Person, personEntity: PersonEntity): PersonChainable {
    val matchingFieldsHaveChanged = personEntity.evaluateMatchingFields {
      it.update(person)
      it.buildChildEntities(person)
    }
    return PersonChainable(
      personEntity = personRepository.save(personEntity),
      matchingFieldsChanged = matchingFieldsHaveChanged,
      linkOnCreate = person.behaviour.linkOnCreate,
    )
  }

  private fun PersonEntity.evaluateMatchingFields(change: (PersonEntity) -> Unit): Boolean {
    val oldMatchingDetails = PersonMatchRecord.from(this)
    change(this)
    return oldMatchingDetails.matchingFieldsAreDifferent(
      PersonMatchRecord.from(
        this,
      ),
    )
  }

  private fun PersonEntity.buildChildEntities(person: Person) {
    this.attachPseudonyms(pseudonymFactory.buildPseudonyms(person))
    this.attachNationalities(nationalityFactory.buildNationalities(person))
    this.attachEthnicity(ethnicityFactory.buildEthnicity(person))
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

  private fun PersonEntity.attachEthnicity(ethnicityCodeEntity: EthnicityCodeEntity?) {
    this.ethnicityCode = ethnicityCodeEntity
  }
}
