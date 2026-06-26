package uk.gov.justice.digital.hmpps.personrecord.service.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDateTime
import kotlin.reflect.KClass

fun PersonEntity.updatePersonEntity(
  person: Person,
  childrenToIgnore: Set<KClass<*>>,
) {
  when (this.sourceSystem) {
    SourceSystemType.NOMIS -> fieldsToUpdatePrison(person)
    SourceSystemType.DELIUS -> fieldsToUpdateProbation(person, childrenToIgnore)
    else -> fieldsToUpdate(person, childrenToIgnore)
  }
}

private fun PersonEntity.fieldsToUpdatePrison(
  person: Person,
) {
  this.defendantId = person.defendantId
  this.crn = person.crn
  this.prisonNumber = person.prisonNumber
  this.masterDefendantId = person.masterDefendantId
  this.cId = person.cId
  this.sexualOrientation = person.sexualOrientation
  this.lastModified = LocalDateTime.now()
  this.dateOfDeath = person.dateOfDeath
  this.ethnicityCode = person.ethnicityCode
  this.genderIdentity = person.genderIdentity
  this.selfDescribedGenderIdentity = person.selfDescribedGenderIdentity
  this.disability = person.disability
  this.immigrationStatus = person.immigrationStatus
  this.birthplace = person.birthplace
  this.birthCountryCode = person.birthCountryCode
  this.nationalityNotes = person.nationalityNotes
  this.updateChildEntities(person)
}

private fun PersonEntity.fieldsToUpdate(
  person: Person,
  childrenToIgnore: Set<KClass<*>>,
) {
  this.defendantId = person.defendantId
  this.crn = person.crn
  this.prisonNumber = person.prisonNumber
  this.masterDefendantId = person.masterDefendantId
  this.religion = person.religion
  this.cId = person.cId
  this.sexualOrientation = person.sexualOrientation
  this.lastModified = LocalDateTime.now()
  this.dateOfDeath = person.dateOfDeath
  this.ethnicityCode = person.ethnicityCode
  this.genderIdentity = person.genderIdentity
  this.selfDescribedGenderIdentity = person.selfDescribedGenderIdentity
  this.disability = person.disability
  this.immigrationStatus = person.immigrationStatus
  this.birthplace = person.birthplace
  this.birthCountryCode = person.birthCountryCode
  this.nationalityNotes = person.nationalityNotes
  this.updateChildEntities(person, childrenToIgnore)
}

private fun PersonEntity.fieldsToUpdateProbation(
  person: Person,
  childrenToIgnore: Set<KClass<*>>,
) {
  this.defendantId = person.defendantId
  this.crn = person.crn
  this.prisonNumber = person.prisonNumber
  this.masterDefendantId = person.masterDefendantId
  this.religion = person.religion
  this.cId = person.cId
  this.sexualOrientation = person.sexualOrientation
  this.lastModified = LocalDateTime.now()
  this.dateOfDeath = person.dateOfDeath
  this.ethnicityCode = person.ethnicityCode
  this.genderIdentity = person.genderIdentity
  this.selfDescribedGenderIdentity = person.selfDescribedGenderIdentity
  this.disability = person.disability
  this.immigrationStatus = person.immigrationStatus
  this.birthplace = person.birthplace
  this.birthCountryCode = person.birthCountryCode
  this.nationalityNotes = person.nationalityNotes
  this.updateChildEntities(person, setOf(AddressEntity::class))
}
