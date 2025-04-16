package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

data class RecordPersonTelemetry(val telemetryEventType: TelemetryEventType, val personEntity: PersonEntity)
