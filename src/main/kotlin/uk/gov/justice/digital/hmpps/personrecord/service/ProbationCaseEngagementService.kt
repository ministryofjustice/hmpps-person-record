package uk.gov.justice.digital.hmpps.personrecord.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.DeliusOffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.DefendantRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OffenderRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

const val DELIUS_EVENT = "DELIUS-EVENT"

@Service
class ProbationCaseEngagementService(
  val defendantRepository: DefendantRepository,
  val offenderRepository: OffenderRepository,
  val personRepository: PersonRepository,
  val telemetryService: TelemetryService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun processNewOffender(newOffenderDetail: DeliusOffenderDetail) {
    newOffenderDetail.identifiers.pnc?.let {
      val pnc = newOffenderDetail.identifiers.pnc
      val matchingOffenders = personRepository.findByOffendersPncNumber(pnc)?.offenders.orEmpty()

      if (matchingOffenders.isNotEmpty()) {
        log.debug("Offenders found for pnc $pnc")
      } else {
        log.debug("No Offenders found for pnc $pnc")
      }
    }
  }

  fun createOffender(deliusOffenderDetail: DeliusOffenderDetail): OffenderEntity {
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

    return offenderRepository.save(newOffender)
  }
}
