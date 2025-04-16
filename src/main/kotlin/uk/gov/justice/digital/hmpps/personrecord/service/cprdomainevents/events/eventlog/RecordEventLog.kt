package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents

data class RecordEventLog(val eventType: CPRLogEvents, val personEntity: PersonEntity)
