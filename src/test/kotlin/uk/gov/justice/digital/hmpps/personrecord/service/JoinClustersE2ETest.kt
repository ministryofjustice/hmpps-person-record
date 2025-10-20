package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType.OVERRIDE_CONFLICT
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetup
import uk.gov.justice.digital.hmpps.personrecord.test.messages.CommonPlatformHearingSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.messages.commonPlatformHearing
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupSentences
import java.time.format.DateTimeFormatter

class JoinClustersE2ETest : E2ETestBase() {

  @Autowired
  private lateinit var personMatchClient: PersonMatchClient

  @Test
  fun `two records match below fracture threshold then the next matches both above join so a cluster of 3 formed`() {
    val crn = randomCrn()
    val cId = randomCId()
    val defendantId = randomDefendantId()

    val pnc1 = randomLongPnc()
    val cro1 = randomCro()
    val cro2 = randomCro()
    val firstName = randomName()
    val middleName = randomName()
    val lastName = randomName()
    val postCode1 = randomPostcode()
    val date1 = randomDate()

    val deliusSetup = ApiResponseSetup(
      crn = crn,
      cro = cro1,
      pnc = pnc1,
      firstName = firstName,
      middleName = null,
      lastName = lastName,
      dateOfBirth = date1,
      addresses = listOf(
        ApiResponseSetupAddress(postcode = postCode1, fullAddress = null),
      ),
      sentences = listOf(),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, deliusSetup)
    val deliusPersonRecord = awaitNotNullPerson { personRepository.findByCrn(crn) }

    publishLibraMessage(libraHearing(cro = cro2, firstName = firstName, foreName2 = middleName, cId = cId, lastName = lastName, postcode = postCode1, dateOfBirth = date1.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
    val libraPersonRecord = awaitNotNullPerson { personRepository.findByCId(cId) }

    awaitAssert {
      val libraVsDeliusShouldFracture = personMatchClient.getPersonScores(libraPersonRecord.matchId.toString())[0].candidateShouldFracture
      assertThat(libraVsDeliusShouldFracture).isTrue()
    }

    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            firstName = firstName,
            middleName = middleName,
            pnc = pnc1,
            cro = cro2,
            lastName = lastName,
            defendantId = defendantId,
            address = CommonPlatformHearingSetupAddress(buildingName = "", buildingNumber = "", thoroughfareName = "", dependentLocality = "", postTown = "", postcode = postCode1),
            dateOfBirth = date1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
          ),
        ),
      ),
    )

    val commonPlatformPersonRecord = awaitNotNullPerson { personRepository.findByDefendantId(defendantId) }
    awaitAssert {
      val commonPlatformMatchId = commonPlatformPersonRecord.matchId.toString()
      val deliusMatchId = deliusPersonRecord.matchId.toString()
      val libraMatchId = libraPersonRecord.matchId.toString()

      val scores = personMatchClient.getPersonScores(commonPlatformMatchId)
      val commonPlatformVsDeliusShouldJoin = scores[0].candidateShouldJoin
      val commonPlatformVsLibraShouldJoin = scores[1].candidateShouldJoin
      assertThat(commonPlatformVsDeliusShouldJoin).isTrue()
      assertThat(commonPlatformVsLibraShouldJoin).isTrue()

      val commonPlatformVsDeliusValid = personMatchClient.isClusterValid(listOf(commonPlatformMatchId, deliusMatchId)).isClusterValid
      val commonPlatformVsLibraValid = personMatchClient.isClusterValid(listOf(commonPlatformMatchId, libraMatchId)).isClusterValid
      val deliusVsLibraValid = personMatchClient.isClusterValid(listOf(deliusMatchId, libraMatchId)).isClusterValid
      assertThat(commonPlatformVsDeliusValid).isTrue()
      assertThat(commonPlatformVsLibraValid).isTrue()
      assertThat(deliusVsLibraValid).isFalse()
    }

    commonPlatformPersonRecord.personKey!!.assertClusterStatus(ACTIVE)
    commonPlatformPersonRecord.personKey!!.assertClusterIsOfSize(3)
  }

  @Test
  fun `should create 2 people who do not match followed by a third which matches both and all end up on same cluster`() {
    val firstCrnWithPnc = randomCrn()
    val secondCrnWithCro = randomCrn()
    val thirdCrnWithBoth = randomCrn()
    val pnc = randomLongPnc()
    val cro = randomCro()
    val basePerson = createRandomProbationPersonDetails(firstCrnWithPnc)
    val firstSetup = ApiResponseSetup(
      crn = firstCrnWithPnc,
      cro = "",
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
      sentences = listOf(ApiResponseSetupSentences(basePerson.sentences.first().sentenceDate)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, firstSetup)

    val firstPersonRecord = awaitNotNullPerson { personRepository.findByCrn(firstCrnWithPnc) }
    assertThat(firstPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(firstPersonRecord.getPnc()).isEqualTo(pnc)
    assertThat(firstPersonRecord.addresses.size).isEqualTo(1)
    firstPersonRecord.personKey?.assertClusterStatus(ACTIVE)
    firstPersonRecord.personKey?.assertClusterIsOfSize(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to firstCrnWithPnc),
    )
    val secondSetup = ApiResponseSetup(
      crn = secondCrnWithCro,
      cro = cro,
      pnc = "",
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, secondSetup)

    val secondPersonRecord = awaitNotNullPerson { personRepository.findByCrn(secondCrnWithCro) }
    assertThat(secondPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(secondPersonRecord.getCro()).isEqualTo(cro)
    secondPersonRecord.personKey?.assertClusterStatus(ACTIVE)
    secondPersonRecord.personKey?.assertClusterIsOfSize(1)
    assertThat(secondPersonRecord.personKey!!.personUUID).isNotEqualTo(firstPersonRecord.personKey!!.personUUID)

    val thirdSetup = ApiResponseSetup(
      crn = thirdCrnWithBoth,
      cro = cro,
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
      sentences = listOf(ApiResponseSetupSentences(basePerson.sentences.first().sentenceDate)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, thirdSetup)

    val thirdPersonRecord = awaitNotNullPerson { personRepository.findByCrn(thirdCrnWithBoth) }
    assertThat(thirdPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(thirdPersonRecord.getCro()).isEqualTo(cro)
    assertThat(thirdPersonRecord.getPnc()).isEqualTo(pnc)
    thirdPersonRecord.personKey?.assertClusterStatus(ACTIVE)
    thirdPersonRecord.personKey?.assertClusterIsOfSize(3)
  }

  @Test
  fun `should create 2 people, one with cro and pnc, one with pnc, which match and end up on same cluster`() {
    val firstCrnWithPnc = randomCrn()
    val secondCrnWithCro = randomCrn()

    val pnc = randomLongPnc()
    val cro = randomCro()
    val basePerson = createRandomProbationPersonDetails(firstCrnWithPnc)
    val firstSetup = ApiResponseSetup(
      crn = firstCrnWithPnc,
      cro = "",
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, firstSetup)

    val firstPersonRecord = awaitNotNullPerson { personRepository.findByCrn(firstCrnWithPnc) }
    assertThat(firstPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(firstPersonRecord.getPnc()).isEqualTo(pnc)
    firstPersonRecord.personKey?.assertClusterIsOfSize(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to firstCrnWithPnc),
    )
    val secondSetup = ApiResponseSetup(
      crn = secondCrnWithCro,
      cro = cro,
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, secondSetup)

    val secondPersonRecord = awaitNotNullPerson { personRepository.findByCrn(secondCrnWithCro) }
    assertThat(secondPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(secondPersonRecord.getCro()).isEqualTo(cro)
    secondPersonRecord.personKey?.assertClusterIsOfSize(2)
    assertThat(secondPersonRecord.personKey!!.personUUID).isEqualTo(firstPersonRecord.personKey!!.personUUID)
  }

  @Test
  fun `should trigger CPRDeliusMergeRequestRaised appInsight event when 2 Delius record join the same cluster`() {
    val firstCrnWithPnc = randomCrn()
    val secondCrnWithCro = firstCrnWithPnc

    val basePerson = createRandomProbationPersonDetails(firstCrnWithPnc)
    val firstSetup = ApiResponseSetup(
      crn = firstCrnWithPnc,
      cro = basePerson.getCro(),
      pnc = basePerson.getPnc(),
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, firstSetup)

    val firstPersonRecord = awaitNotNullPerson { personRepository.findByCrn(firstCrnWithPnc) }
    assertThat(firstPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(firstPersonRecord.getPnc()).isEqualTo(pnc)
    firstPersonRecord.personKey?.assertClusterIsOfSize(1)

    checkTelemetry(
      CPR_RECORD_CREATED,
      mapOf("SOURCE_SYSTEM" to "DELIUS", "CRN" to firstCrnWithPnc),
    )
    val secondSetup = ApiResponseSetup(
      crn = secondCrnWithCro,
      cro = cro,
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, secondSetup)

    val secondPersonRecord = awaitNotNullPerson { personRepository.findByCrn(secondCrnWithCro) }
    assertThat(secondPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(secondPersonRecord.getCro()).isEqualTo(cro)
    secondPersonRecord.personKey?.assertClusterIsOfSize(2)
    assertThat(secondPersonRecord.personKey!!.personUUID).isEqualTo(firstPersonRecord.personKey!!.personUUID)
  }

  @Test
  fun `handle a new person which matches two clusters with override markers`() {
    val firstCrn = randomCrn()
    val secondCrn = randomCrn()
    val thirdCrn = randomCrn()

    val pnc = randomLongPnc()
    val cro = randomCro()
    val basePerson = createRandomProbationPersonDetails(firstCrn)
    val firstSetup = ApiResponseSetup(
      crn = firstCrn,
      cro = cro,
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
      sentences = listOf(ApiResponseSetupSentences(basePerson.sentences.first().sentenceDate)),
    )

    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, firstSetup)
    var firstPersonRecord = awaitNotNullPerson { personRepository.findByCrn(firstCrn) }
    assertThat(firstPersonRecord.personKey!!.personEntities.size).isEqualTo(1)

    val secondSetup = ApiResponseSetup(
      crn = secondCrn,
      cro = cro,
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
      sentences = listOf(ApiResponseSetupSentences(basePerson.sentences.first().sentenceDate)),
    )
    probationUnmergeEventAndResponseSetup(OFFENDER_UNMERGED, secondCrn, firstCrn, reactivatedSetup = secondSetup, unmergedSetup = firstSetup)
    awaitAssert { assertThat(personRepository.findByCrn(secondCrn)?.personKey).isNotNull() }
    var secondPersonRecord = awaitNotNullPerson { personRepository.findByCrn(secondCrn) }
    assertThat(secondPersonRecord.personKey!!.personEntities.size).isEqualTo(1)

    secondPersonRecord.assertExcluded(firstPersonRecord)

    val thirdSetup = ApiResponseSetup(
      crn = thirdCrn,
      cro = cro,
      pnc = pnc,
      firstName = basePerson.firstName,
      middleName = basePerson.middleNames,
      lastName = basePerson.lastName,
      dateOfBirth = basePerson.dateOfBirth,
      addresses = listOf(ApiResponseSetupAddress(postcode = basePerson.addresses.first().postcode, fullAddress = randomFullAddress())),
      aliases = listOf(ApiResponseSetupAlias(firstName = basePerson.aliases.first().firstName!!, middleName = basePerson.aliases.first().middleNames!!, lastName = basePerson.aliases.first().lastName!!, dateOfBirth = basePerson.aliases.first().dateOfBirth!!)),
      sentences = listOf(ApiResponseSetupSentences(basePerson.sentences.first().sentenceDate)),
    )

    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, thirdSetup)

    val thirdPersonRecord = awaitNotNullPerson { personRepository.findByCrn(thirdCrn) }
    thirdPersonRecord.personKey!!.assertClusterStatus(NEEDS_ATTENTION, OVERRIDE_CONFLICT)
    thirdPersonRecord.personKey!!.assertClusterIsOfSize(1)

    checkEventLog(thirdCrn, CPRLogEvents.CPR_RECORD_CREATED) { eventLogs ->
      assertThat(eventLogs).hasSize(1)
      assertThat(eventLogs.first().uuidStatusType).isEqualTo(NEEDS_ATTENTION)
    }

    secondPersonRecord = awaitNotNullPerson { personRepository.findByCrn(secondCrn) }
    secondPersonRecord.personKey!!.assertClusterStatus(ACTIVE)
    secondPersonRecord.personKey!!.assertClusterIsOfSize(1)

    firstPersonRecord = awaitNotNullPerson { personRepository.findByCrn(firstCrn) }
    firstPersonRecord.personKey!!.assertClusterStatus(ACTIVE)
    firstPersonRecord.personKey!!.assertClusterIsOfSize(1)
  }
}
