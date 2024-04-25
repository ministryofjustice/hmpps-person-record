package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class ProbationCaseEngagementService(
  val offenderRepository: OffenderRepository,
  val telemetryService: TelemetryService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processNewOffender(newOffenderDetail: DeliusOffenderDetail) {
    process(newOffenderDetail)
  }

  private fun process(newOffenderDetail: DeliusOffenderDetail) {
    val offenders = offenderRepository.findAllByCrn(newOffenderDetail.identifiers.crn)
    if (offenders.isNullOrEmpty()) {
      createOffender(newOffenderDetail)
    } else {
      log.info("Offender already exists, for crn: ${newOffenderDetail.identifiers.crn}")
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun trackEvent(eventType: TelemetryEventType, crn: String, pnc: String? = null, uuid: String? = null) {
    telemetryService.trackEvent(
      eventType,
      mapOf(
        "UUID" to uuid,
        "CRN" to crn,
        "PNC" to pnc,
      ).filterValues { it != null } as Map<String, String>,
    )
  }

  private fun createOffender(deliusOffenderDetail: DeliusOffenderDetail) {
    trackEvent(TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC, deliusOffenderDetail.identifiers.crn, deliusOffenderDetail.identifiers.pnc)
    val offenderEntity = OffenderEntity(
      crn = deliusOffenderDetail.identifiers.crn,
      pncNumber = PNCIdentifier.from(deliusOffenderDetail.identifiers.pnc),
      firstName = deliusOffenderDetail.name.forename,
      lastName = deliusOffenderDetail.name.surname,
      dateOfBirth = deliusOffenderDetail.dateOfBirth,
    )
    offenderRepository.saveAndFlush(offenderEntity)
  }
}
