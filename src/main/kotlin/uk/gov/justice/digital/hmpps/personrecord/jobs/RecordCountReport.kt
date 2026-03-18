package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationEventPublisher
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@RestController
class RecordCountReport(
  private val personRepository: PersonRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/recordcountreport"])
  suspend fun collectAndReport(): String {
    collectAndReportStats()
    return OK
  }

  suspend fun collectAndReportStats() {
    CoroutineScope(Dispatchers.Default).launch {
      applicationEventPublisher.publishEvent(
        RecordTelemetry(
          TelemetryEventType.CPR_RECORD_COUNT_REPORT,
          mapOf(
            EventKeys.NOMIS to personRepository.countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(SourceSystemType.NOMIS).toString(),
            EventKeys.DELIUS to personRepository.countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(SourceSystemType.DELIUS).toString(),
            EventKeys.LIBRA to personRepository.countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(SourceSystemType.LIBRA).toString(),
            EventKeys.COMMON_PLATFORM to personRepository.countBySourceSystemAndMergedToIsNullAndPassiveStateFalse(SourceSystemType.COMMON_PLATFORM).toString(),
          ),
        ),
      )
    }
  }

  companion object {
    private const val OK = "OK"
  }
}
