package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.updater

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import java.time.LocalDateTime
import kotlin.reflect.KClass

class OtherPersonUpdater : PersonUpdater {
  override fun update(
    person: Person,
    childrenToIgnore: Set<KClass<*>>,
    personEntity: PersonEntity,
  ) {
    personEntity.defendantId = person.defendantId
    personEntity.crn = person.crn
    personEntity.prisonNumber = person.prisonNumber
    personEntity.masterDefendantId = person.masterDefendantId
    personEntity.religion = person.religion
    personEntity.cId = person.cId
    personEntity.sexualOrientation = person.sexualOrientation
    personEntity.lastModified = LocalDateTime.now()
    personEntity.dateOfDeath = person.dateOfDeath
    personEntity.ethnicityCode = person.ethnicityCode
    personEntity.genderIdentity = person.genderIdentity
    personEntity.selfDescribedGenderIdentity = person.selfDescribedGenderIdentity
    personEntity.disability = person.disability
    personEntity.immigrationStatus = person.immigrationStatus
    personEntity.birthplace = person.birthplace
    personEntity.birthCountryCode = person.birthCountryCode
    personEntity.nationalityNotes = person.nationalityNotes
    personEntity.updateChildEntities(person, childrenToIgnore)
  }
}
