package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

class PersonChangeChecker(originalPersonEntity: PersonEntity) {
  val originalPerson = Person.from(originalPersonEntity)
  val originalMatchRecord = PersonMatchRecord.from(originalPersonEntity)

  fun anyFieldsHaveChanged(newPerson: PersonEntity): Boolean = originalPerson != Person.from(newPerson)

  fun matchingFieldsHaveChanged(newPerson: PersonEntity): Boolean = originalMatchRecord.matchingFieldsAreDifferent(newPerson)
}
