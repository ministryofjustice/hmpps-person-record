package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class PersonCreated(val personEntity: PersonEntity)
