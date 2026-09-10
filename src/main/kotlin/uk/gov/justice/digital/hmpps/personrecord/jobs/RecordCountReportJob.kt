package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.telemetry.RecordTelemetry
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Component
@ConditionalOnProperty(name = ["batch.enabled"], havingValue = "true")
class RecordCountReportJob(
  private val personRepository: PersonRepository,
  private val applicationEventPublisher: ApplicationEventPublisher,
): BatchJob {
  override val jobName = "RECORD_COUNT_REPORT"

  override fun run() {
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
