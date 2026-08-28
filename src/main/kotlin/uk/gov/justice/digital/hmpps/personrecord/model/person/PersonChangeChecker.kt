package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

class PersonChangeChecker(originalPersonEntity: PersonEntity) {
  val originalMatchRecord = PersonMatchRecord.from(originalPersonEntity)

  fun matchingFieldsHaveChanged(updatedPersonEntity: PersonEntity): Boolean = originalMatchRecord.matchingFieldsAreDifferent(updatedPersonEntity)
}
