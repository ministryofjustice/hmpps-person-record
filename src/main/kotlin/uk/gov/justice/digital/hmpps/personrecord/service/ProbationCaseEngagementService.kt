package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType

const val DELIUS_EVENT = "DELIUS-EVENT"

@Service
class ProbationCaseEngagementService(
  val offenderRepository: OffenderRepository,
  val personRepository: PersonRepository,
  val telemetryService: TelemetryService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processNewOffender(newOffenderDetail: DeliusOffenderDetail) {
    val crn = newOffenderDetail.identifiers.crn
    newOffenderDetail.identifiers.pnc?.let {
      val pnc = newOffenderDetail.identifiers.pnc

      val existingPerson = personRepository.findByOffendersPncNumber(pnc)

      existingPerson?.let { // person exist for pnc
        log.debug("Person record exist for pnc $pnc - adding new offender to person with crn $crn")
        addOffenderToPerson(it, createOffender(newOffenderDetail))

        telemetryService.trackEvent(
          TelemetryEventType.NEW_DELIUS_RECORD_PNC_MATCHED,
          mapOf(
            "CRN" to crn,
            "PNC" to pnc,
          ),
        )
      } ?: { // no person for pnc
        log.debug("Person not exist for pnc $pnc - creating a new person and offender")

        val newPerson = createNewPersonAndOffender(newOffenderDetail)

        telemetryService.trackEvent(
          TelemetryEventType.NEW_DELIUS_RECORD_NEW_PNC,
          mapOf(
            "UUID" to newPerson.personId.toString(),
            "CRN" to crn,
            "PNC" to pnc,
          ),
        )
      }
    } ?: { // no pnc
      log.debug("Pnc not present no further processing needed")

      telemetryService.trackEvent(
        TelemetryEventType.NEW_DELIUS_RECORD_NO_PNC,
        mapOf(
          "CRN" to crn,
        ),
      )
    }
  }

  private fun createOffender(deliusOffenderDetail: DeliusOffenderDetail): OffenderEntity {
    log.debug("Creating new offender with pnc  ${deliusOffenderDetail.identifiers.pnc}")
    val newOffender = OffenderEntity(
      crn = deliusOffenderDetail.identifiers.crn,
      pncNumber = deliusOffenderDetail.identifiers.pnc,
      firstName = deliusOffenderDetail.name.forename,
      lastName = deliusOffenderDetail.name.surname,
      dateOfBirth = deliusOffenderDetail.dateOfBirth,
    )
    newOffender.createdBy = DELIUS_EVENT
    newOffender.lastUpdatedBy = DELIUS_EVENT

    return newOffender
  }

  private fun createNewPersonAndOffender(offenderDetail: DeliusOffenderDetail): PersonEntity {
    val newPersonEntity = PersonEntity.new()
    val newOffenderEntity = createOffender(offenderDetail)
    newOffenderEntity.person = newPersonEntity
    newPersonEntity.offenders.add(newOffenderEntity)
    return addOffenderToPerson(newPersonEntity, newOffenderEntity)
  }

  private fun addOffenderToPerson(personEntity: PersonEntity, offenderEntity: OffenderEntity): PersonEntity {
    offenderEntity.person = personEntity
    personEntity.offenders.add(offenderEntity)
    return personRepository.saveAndFlush(personEntity)
  }
}
