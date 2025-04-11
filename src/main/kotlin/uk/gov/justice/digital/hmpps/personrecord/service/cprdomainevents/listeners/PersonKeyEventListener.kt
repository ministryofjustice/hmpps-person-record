package uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.listeners

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.PersonKeyFound
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_CANDIDATE_RECORD_FOUND_UUID
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_UUID_CREATED

@Component
class PersonKeyEventListener(
  private val telemetryService: TelemetryService,
) {

  @EventListener
  fun handlePersonKeyCreated(personKeyCreated: PersonKeyCreated) {
    telemetryService.trackPersonEvent(
      CPR_UUID_CREATED,
      personKeyCreated.personEntity,
      mapOf(EventKeys.UUID to personKeyCreated.personKeyEntity.personId.toString()),
    )
  }

  @EventListener
  fun handlePersonKeyFound(personKeyFound: PersonKeyFound) {
    telemetryService.trackPersonEvent(
      CPR_CANDIDATE_RECORD_FOUND_UUID,
      personKeyFound.personEntity,
      mapOf(
        EventKeys.UUID to personKeyFound.personKeyEntity.personId?.toString(),
        EventKeys.CLUSTER_SIZE to personKeyFound.personKeyEntity.personEntities.size.toString(),
      ),
    )
  }
}
