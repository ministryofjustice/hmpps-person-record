package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class ProbationCaseEngagementService(
  val telemetryService: TelemetryService,
  val personRecordService: PersonRecordService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processNewOffender(newOffenderDetail: DeliusOffenderDetail) {
    process(newOffenderDetail)
  }

  private fun process(newOffenderDetail: DeliusOffenderDetail) {
    val existingPeople = personRecordService.findPersonRecordsByPnc(PNCIdentifier.from(newOffenderDetail.identifiers.pnc!!))
    if (existingPeople.isEmpty()) {
      handleNoPersonForPnc(newOffenderDetail)
    } else {
      handlePersonExistsForPnc(newOffenderDetail, existingPeople)
    }
  }

  private fun handlePersonExistsForPnc(newOffenderDetail: DeliusOffenderDetail, personRecords: List<PersonEntity>) {
    val pnc = newOffenderDetail.identifiers.pnc!!
    val crn = newOffenderDetail.identifiers.crn
    log.debug("Person record exists for pnc $pnc - adding new offender to person with crn $crn")
    // should we not check that there is no offender first?
    personRecordService.addOffenderToPerson(personRecords.get(0), createOffender(newOffenderDetail))
    personRecords.subList(1, personRecords.size).forEach { log.info("Additional matching person records found with id ${it.personId}") }
    trackEvent(TelemetryEventType.NEW_DELIUS_RECORD_PNC_MATCHED, crn, pnc)
  }

  private fun handleNoPersonForPnc(newOffenderDetail: DeliusOffenderDetail) {
    val pnc = newOffenderDetail.identifiers.pnc!!
    val crn = newOffenderDetail.identifiers.crn
    log.debug("Person record does not exist for pnc $pnc - creating a new person and offender")
    val newPerson = createNewPersonAndOffenderFromPnc(newOffenderDetail)
    trackEvent(TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC, crn, pnc, newPerson.personId.toString())
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

  private fun createNewPersonAndOffenderFromPnc(newOffenderDetail: DeliusOffenderDetail): PersonEntity {
    val newOffenderEntity = createOffender(newOffenderDetail)
    return personRecordService.addOffenderToPerson(PersonEntity.new(), newOffenderEntity)
  }

  private fun createOffender(deliusOffenderDetail: DeliusOffenderDetail): OffenderEntity =
    OffenderEntity(
      crn = deliusOffenderDetail.identifiers.crn,
      pncNumber = PNCIdentifier.from(deliusOffenderDetail.identifiers.pnc),
      firstName = deliusOffenderDetail.name.forename,
      lastName = deliusOffenderDetail.name.surname,
      dateOfBirth = deliusOffenderDetail.dateOfBirth,
    )
}
