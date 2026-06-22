package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated

interface PersonEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(personCreated: PersonCreated)
  fun onUpdate(personUpdated: PersonUpdated)
  fun onDelete(personDeleted: PersonDeleted)
}
