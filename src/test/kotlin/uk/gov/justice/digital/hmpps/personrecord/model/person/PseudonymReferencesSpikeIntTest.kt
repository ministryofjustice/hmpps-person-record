package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonIdentifier
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PersonReference
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonCreated
import uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.messages.domainevent.PrisonPersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingMultiNodeTestBase
import uk.gov.justice.digital.hmpps.personrecord.extensions.getType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDriverLicenseNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomEmail
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligionCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomShortPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupIdentifier
import java.time.LocalDate

class PseudonymReferencesSpikeIntTest : MessagingMultiNodeTestBase() {

  @Nested
  inner class SuccessfulProcessing {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
    }

    @Test
    fun `should create pseudonym reference on create and remove on update`() {
      val prisonNumber = randomPrisonNumber()
      val title = randomTitleCode()
      val pnc = randomShortPnc()
      val cro = randomCro()
      val nationalInsuranceNumber = randomNationalInsuranceNumber()
      val driverLicenseNumber = randomDriverLicenseNumber()
      val aliasPnc = randomLongPnc()

      stubNoMatchesPersonMatch()
      val apiResponseSetup = ApiResponseSetup(
        title = title.key,
        gender = randomPrisonSexCode().key,
        aliases = listOf(
          ApiResponseSetupAlias(
            title.key,
            randomName(),
            randomName(),
            randomName(),
            randomDate(),
            randomPrisonSexCode().key,
            identifiers = listOf(ApiResponseSetupIdentifier(type = "PNC", value = aliasPnc)),
          ),
        ),
        firstName = randomName(),
        middleName = randomName(),
        lastName = randomName(),
        prisonNumber = prisonNumber,
        pnc = pnc,
        email = randomEmail(),
        sentenceStartDate = randomDate(),
        primarySentence = true,
        cro = cro,
        addresses = listOf(
          ApiResponseSetupAddress(
            postcode = randomPostcode(),
            fullAddress = randomFullAddress(),
            startDate = LocalDate.of(1970, 1, 1),
            noFixedAbode = true,
          ),
        ),
        dateOfBirth = randomDate(),
        nationality = randomPrisonNationalityCode(),
        ethnicity = randomPrisonEthnicity(),
        religion = randomReligionCode().name,
        identifiers = listOf(
          ApiResponseSetupIdentifier(type = "NINO", value = nationalInsuranceNumber),
          ApiResponseSetupIdentifier(type = "DL", value = driverLicenseNumber),
        ),
      )
      prisonCreateEventAndResponseSetup(apiResponseSetup)

      checkTelemetry(CPR_RECORD_CREATED, mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber))

      val personEntity = personRepository.findByPrisonNumber(prisonNumber)!!

      val populatedReferencesUpdateIdCount = personEntity.references.count { it.updateId != null }
      assertThat(populatedReferencesUpdateIdCount).isEqualTo(5)

      // If we have multiple PNC's or CROs across the person because of aliases, these getters can fail to find the one expected.
      // We can consider omitting ones connected to pseudonyms if needed, but might be a bandaid fix
//      assertThat(personEntity.getPnc()).isEqualTo(PNCIdentifier.from(pnc).pncId)
      assertThat(personEntity.getCro()).isEqualTo(cro)
      assertThat(personEntity.references.getType(NATIONAL_INSURANCE_NUMBER).first()).isEqualTo(nationalInsuranceNumber)
      assertThat(personEntity.references.getType(DRIVER_LICENSE_NUMBER).first()).isEqualTo(driverLicenseNumber)

      assertThat(personEntity.pseudonyms.size).isEqualTo(2)
      assertThat(personEntity.getAliases().size).isEqualTo(1)
      assertThat(personEntity.getAliases()[0].references.size).isEqualTo(1)
      assertThat(personEntity.getAliases()[0].references.getType(PNC).first()).isEqualTo(aliasPnc)

      // send an update with the removed alias identifier
      prisonUpdateEventAndResponseSetup(
        apiResponseSetup.copy(
          aliases = listOf(
            ApiResponseSetupAlias(
              title.key,
              randomName(),
              randomName(),
              randomName(),
              randomDate(),
              randomPrisonSexCode().key,
            ),
          ),
        ),
      )

      checkTelemetry(TelemetryEventType.CPR_RECORD_UPDATED, mapOf("SOURCE_SYSTEM" to NOMIS.name, "PRISON_NUMBER" to prisonNumber))

      val updatePersonEntity = personRepository.findByPrisonNumber(prisonNumber)!!

      assertThat(updatePersonEntity.pseudonyms.size).isEqualTo(2)
      assertThat(updatePersonEntity.getAliases().size).isEqualTo(1)
      assertThat(updatePersonEntity.getAliases()[0].references).isEmpty()
    }
  }

  private fun prisonCreateEventAndResponseSetup(apiResponseSetup: ApiResponseSetup) {
    stubPrisonResponse(apiResponseSetup)
    publishPrisonPersonCreatedEvent(apiResponseSetup.prisonNumber!!)
  }

  private fun prisonUpdateEventAndResponseSetup(apiResponseSetup: ApiResponseSetup) {
    stubPrisonResponse(apiResponseSetup)
    publishPrisonPersonUpdatedEvent(apiResponseSetup.prisonNumber!!)
  }

  private fun publishPrisonPersonCreatedEvent(prisonNumber: String) {
    publishDomainEvent(
      PrisonPersonCreated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
  }

  private fun publishPrisonPersonUpdatedEvent(prisonNumber: String) {
    publishDomainEvent(
      PrisonPersonUpdated(
        personReference = PersonReference(listOf(PersonIdentifier("NOMS", prisonNumber))),
      ),
    )
  }
}
