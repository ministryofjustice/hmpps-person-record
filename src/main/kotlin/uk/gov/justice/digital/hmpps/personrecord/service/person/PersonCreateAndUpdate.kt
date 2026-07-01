package uk.gov.justice.digital.hmpps.personrecord.service.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDateTime

fun PersonEntity.updatePersonEntity(
  person: Person,
): PersonEntity {
  when (this.sourceSystem) {
    NOMIS -> fieldsToUpdatePrison(person)
    DELIUS -> fieldsToUpdateProbation(person)
    else -> fieldsToUpdate(person)
  }
  return this
}

private fun PersonEntity.fieldsToUpdatePrison(person: Person) {
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

private fun PersonEntity.fieldsToUpdate(person: Person) {
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
  this.updateChildEntities(person)
}

private fun PersonEntity.fieldsToUpdateProbation(person: Person) {
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
