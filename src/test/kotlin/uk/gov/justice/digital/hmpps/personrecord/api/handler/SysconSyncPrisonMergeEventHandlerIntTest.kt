package uk.gov.justice.digital.hmpps.personrecord.api.handler

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest
import uk.gov.justice.digital.hmpps.personrecord.api.controller.prison.SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest
import uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon.PrisonMergeEventHandler
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAliasesAndIdentifiersRequest
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonMerge
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.prison.PrisonReligionRepository
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.AGNO
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode.CALV
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import java.time.LocalDate

class SysconSyncPrisonMergeEventHandlerIntTest(
  @Autowired private val prisonMergeEventHandler: PrisonMergeEventHandler,
  @Autowired private val prisonReligionRepository: PrisonReligionRepository,
) : MessagingTestBase() {

  @Nested
  inner class MergeAddressesAndContacts {
    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `Should return mappings for the merge`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()
      createPersonKey()
        .addPerson(Person(prisonNumber = toPrisonNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = fromPrisonNumber, sourceSystem = NOMIS))
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))
      val response = prisonMergeEventHandler.handleMerge(toPrisonNumber = toPrisonNumber, prisonMerge = validPrisonMerge(fromPrisonNumber))
      assertThat(response.toPrisonNumber).isEqualTo(toPrisonNumber)
    }
  }

  @Nested
  inner class MergeAliasesAndIdentifiers {
    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `Should replace the aliases and identifiers of the to person with those in the request`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()
      createPersonKey()
        .addPerson(createRandomPrisonPersonDetails(toPrisonNumber))
        .addPerson(createRandomPrisonPersonDetails(fromPrisonNumber))
      val toPseudonymIdsBeforeMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!.pseudonyms.map { it.updateId }
      val fromAliasToDonate = personRepository.findByPrisonNumber(fromPrisonNumber)!!.getAliases().single()
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))

      val request = SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest.validPrisonPseudoymnAndReferencesRequest
      val donatedAliasRequest = request.aliases.first { !it.isPrimary }.copy(cprAliasId = fromAliasToDonate.updateId.toString())
      val primaryAliasRequest = request.aliases.first { it.isPrimary }
      val prisonMerge = validPrisonMerge(fromPrisonNumber).copy(
        toPrisonAliasesAndIdentifiersRequest = PrisonAliasesAndIdentifiersRequest(aliases = listOf(primaryAliasRequest, donatedAliasRequest)),
      )

      val response = prisonMergeEventHandler.handleMerge(toPrisonNumber = toPrisonNumber, prisonMerge = prisonMerge)

      val aliasMappings = response.sysconAliasesAndIdentifiersResponseBody!!.aliasesMappings
      assertThat(response.sysconAliasesAndIdentifiersResponseBody.prisonNumber).isEqualTo(toPrisonNumber)
      assertThat(aliasMappings.map { it.nomisOffenderId }).containsExactly(primaryAliasRequest.nomisOffenderId, donatedAliasRequest.nomisOffenderId)
      assertThat(aliasMappings[1].cprAliasId).isEqualTo(fromAliasToDonate.updateId.toString())
      assertThat(aliasMappings[0].identifierMappings).hasSize(primaryAliasRequest.identifiers.size)
      assertThat(aliasMappings[1].identifierMappings).hasSize(donatedAliasRequest.identifiers.size)

      val toPersonAfterMerge = personRepository.findByPrisonNumber(toPrisonNumber)!!
      assertThat(toPersonAfterMerge.pseudonyms.map { it.updateId.toString() }).containsExactlyInAnyOrderElementsOf(aliasMappings.map { it.cprAliasId })
      assertThat(toPersonAfterMerge.pseudonyms.map { it.updateId }).doesNotContainAnyElementsOf(toPseudonymIdsBeforeMerge)
      assertThat(toPersonAfterMerge.getPrimaryName().firstName).isEqualTo(primaryAliasRequest.firstName)
      assertThat(toPersonAfterMerge.getAliases().single().firstName).isEqualTo(donatedAliasRequest.firstName)
      assertThat(toPersonAfterMerge.references.map { it.updateId.toString() })
        .containsExactlyInAnyOrderElementsOf(aliasMappings.flatMap { it.identifierMappings }.map { it.cprIdentifierId })
    }
  }

  @Nested
  inner class MergingReligion {

    @BeforeEach
    fun beforeEach() {
      stubPersonMatchUpsert()
      stubPersonMatchScores()
      stubDeletePersonMatch()
    }

    @Test
    fun `Should merge the religions to the to person`() {
      val toPrisonNumber = randomPrisonNumber()
      val fromPrisonNumber = randomPrisonNumber()
      createPersonKey()
        .addPerson(Person(prisonNumber = toPrisonNumber, sourceSystem = NOMIS))
        .addPerson(Person(prisonNumber = fromPrisonNumber, sourceSystem = NOMIS))
      prisonReligionRepository.saveAll(
        listOf(
          prisonReligionEntity(
            prisonNumber = toPrisonNumber,
            startDate = LocalDate.of(2021, 1, 1),
            code = CALV,
          ),
          prisonReligionEntity(
            prisonNumber = fromPrisonNumber,
            startDate = LocalDate.of(2021, 1, 25),
            code = AGNO,
          ),
        ),
      )
      stubPrisonResponse(ApiResponseSetup(prisonNumber = toPrisonNumber))

      // Method under test
      prisonMergeEventHandler.handleMerge(toPrisonNumber = toPrisonNumber, prisonMerge = validPrisonMerge(fromPrisonNumber))

      assertThat(personRepository.findByPrisonNumber(toPrisonNumber)?.religion).isEqualTo(AGNO)
      assertThat(
        prisonReligionRepository.findByPrisonNumberOrderByStartDateDescCreateDateTimeDesc(
          toPrisonNumber,
        ),
      )
        .satisfiesExactly(
          {
            assertThat(it.code).isEqualTo(AGNO)
            assertThat(it.prisonRecordType).isEqualTo(PrisonRecordType.CURRENT)
          },
          {
            assertThat(it.code).isEqualTo(CALV)
            assertThat(it.prisonRecordType).isEqualTo(PrisonRecordType.HISTORIC)
          },
        )
    }
  }

  private fun validPrisonMerge(fromPrisonNumber: String) = PrisonMerge(
    fromPrisonNumber = fromPrisonNumber,
    toPrisonAddressesAndContactsRequest = SysconSyncPrisonAddressesContactsMigrationAPIControllerIntTest.validPrisonAddressesAndContactsRequest,
    toPrisonAliasesAndIdentifiersRequest = SysconSyncPrisonAliasesIdentifiersAPIControllerIntTest.validPrisonPseudoymnAndReferencesRequest,
  )
}
