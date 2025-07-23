package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles
import uk.gov.justice.digital.hmpps.personrecord.api.model.admin.AdminEventLogSummary
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import java.util.UUID

@RestController
class EventLogController(
  private val eventLogRepository: EventLogRepository,
) {

  @Hidden
  @PreAuthorize("hasRole('${Roles.PERSON_RECORD_ADMIN_READ_ONLY}')")
  @GetMapping("/admin/event-log/{uuid}")
  suspend fun getEventLogForCluster(
    @PathVariable(name = "uuid") uuid: UUID,
  ): AdminEventLogSummary {
    val eventLogs = withContext(Dispatchers.IO) { eventLogRepository.findAllByPersonUUIDOrderByEventTimestampDesc(uuid) } ?: emptyList()
    return AdminEventLogSummary.from(uuid, eventLogs)
  }
}
