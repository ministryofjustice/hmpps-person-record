package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.publishers

import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated

interface PersonEventPublisher {
  val sourceSystemType: SourceSystemType
  fun onCreate(personCreated: PersonCreated)
}
