package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers.person

import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonDeleted
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated

interface PersonCreatedEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(personCreated: PersonCreated)
}

interface PersonUpdatedEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onUpdate(personUpdated: PersonUpdated)
}

interface PersonDeletedEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onDelete(personDeleted: PersonDeleted)
}
