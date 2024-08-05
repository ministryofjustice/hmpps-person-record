package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.SQSMessage
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.CourtHearingEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.CourtMessageEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.CourtHearingRepository
import uk.gov.justice.digital.hmpps.personrecord.service.EventKeys
import uk.gov.justice.digital.hmpps.personrecord.service.TelemetryService
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class TempHearingService(
  val telemetryService: TelemetryService,
  val courtHearingRepository: CourtHearingRepository,
) {

  @Transactional
  fun saveHearing(sqsMessage: SQSMessage, commonPlatformHearingEvent: CommonPlatformHearingEvent) {
    val hearingId = commonPlatformHearingEvent.hearing.id

    val existingHearing = courtHearingRepository.findByHearingId(hearingId = hearingId)
    val courtMessageEntity = CourtMessageEntity(messageId = sqsMessage.messageId, message = sqsMessage.message)

    if (existingHearing != null) {
      courtMessageEntity.courtHearing = existingHearing
      existingHearing.messages.add(courtMessageEntity)
      telemetryService.trackEvent(
        TelemetryEventType.FIFO_HEARING_UPDATED,
        mapOf(
          EventKeys.HEARING_ID to hearingId,
        ),
      )
      courtHearingRepository.save(existingHearing)
    } else {
      val courtHearingEntity = CourtHearingEntity(hearingId = hearingId)

      courtMessageEntity.courtHearing = courtHearingEntity
      courtHearingEntity.messages.add(courtMessageEntity)
      telemetryService.trackEvent(
        TelemetryEventType.FIFO_HEARING_CREATED,
        mapOf(
          EventKeys.HEARING_ID to hearingId,
        ),
      )
      courtHearingRepository.save(courtHearingEntity)
    }
  }
}
