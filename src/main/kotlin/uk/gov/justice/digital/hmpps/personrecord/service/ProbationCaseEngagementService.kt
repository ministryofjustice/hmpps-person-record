package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

@Service
class ProbationCaseEngagementService(
  val personRepository: PersonRepository,
  val telemetryService: TelemetryService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processNewOffender(newOffenderDetail: DeliusOffenderDetail) {
    newOffenderDetail.identifiers.pnc?.let {
      handlePncPresent(newOffenderDetail)
    } ?: handleNoPnc()
  }

  private fun handlePncPresent(newOffenderDetail: DeliusOffenderDetail) {
    val existingPerson = personRepository.findPersonEntityByPncNumber(PNCIdentifier.from(newOffenderDetail.identifiers.pnc!!))
    existingPerson?.let { person ->
      handlePersonExistsForPnc(newOffenderDetail, person)
    } ?: handleNoPersonForPnc(newOffenderDetail)
  }

  private fun handlePersonExistsForPnc(newOffenderDetail: DeliusOffenderDetail, person: PersonEntity) {
    val pnc = newOffenderDetail.identifiers.pnc!!
    val crn = newOffenderDetail.identifiers.crn
    log.debug("Person record exists for pnc $pnc - adding new offender to person with crn $crn")
    // should we not check that there is no offender first?
    addOffenderToPerson(person, createOffender(newOffenderDetail))
    trackEvent(TelemetryEventType.NEW_DELIUS_RECORD_PNC_MATCHED, crn, pnc)
  }

  private fun handleNoPersonForPnc(newOffenderDetail: DeliusOffenderDetail) {
    val pnc = newOffenderDetail.identifiers.pnc!!
    val crn = newOffenderDetail.identifiers.crn
    log.debug("Person record does not exist for pnc $pnc - creating a new person and offender")
    val newPerson = createNewPersonAndOffenderFromPnc(newOffenderDetail)
    trackEvent(TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC, crn, pnc, newPerson.personId.toString())
  }

  private fun handleNoPnc() {
    log.debug("Pnc not present no further processing needed")
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
    return addOffenderToPerson(PersonEntity.new(), newOffenderEntity)
  }

  private fun createOffender(deliusOffenderDetail: DeliusOffenderDetail): OffenderEntity {
    log.debug("Creating new offender with pnc  ${deliusOffenderDetail.identifiers.pnc}")
    val newOffender = OffenderEntity(
      crn = deliusOffenderDetail.identifiers.crn,
      pncNumber = PNCIdentifier.from(deliusOffenderDetail.identifiers.pnc),
      firstName = deliusOffenderDetail.name.forename,
      lastName = deliusOffenderDetail.name.surname,
      dateOfBirth = deliusOffenderDetail.dateOfBirth,
    )
    return newOffender
  }

  private fun addOffenderToPerson(personEntity: PersonEntity, offenderEntity: OffenderEntity): PersonEntity {
    offenderEntity.person = personEntity
    personEntity.offenders.add(offenderEntity)
    return personRepository.saveAndFlush(personEntity)
  }
}
