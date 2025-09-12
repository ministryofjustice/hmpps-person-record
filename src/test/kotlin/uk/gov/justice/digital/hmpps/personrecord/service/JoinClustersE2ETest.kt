package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.PersonMatchClient
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.ACTIVE
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION_EXCLUDE
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
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupSentences
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class JoinClustersE2ETest() : E2ETestBase() {

  @Autowired
  private lateinit var personMatchClient: PersonMatchClient

  @Test
  fun `should duplicate the scenario from prod`() {
    val deliusRecordCrn = randomCrn()
    val pnc = randomPnc()
    val cro = randomCro()
    val defendantId = randomDefendantId()
    val firstName = randomName()
    val lastName = randomName()
    val postCode1 = randomPostcode()
    val postCode2 = randomPostcode()
    val postCode3 = randomPostcode()
    val date1 = randomDate()
    val date2 = randomDate()
    val date3 = randomDate()
    // create Delius record
    createRandomProbationPersonDetails(deliusRecordCrn)
    val deliusSetup = ApiResponseSetup(
      crn = deliusRecordCrn,
      cro = cro,
      pnc = pnc,
      firstName = firstName,
      middleName = null,
      lastName = lastName,
      dateOfBirth = date1,
      addresses = listOf(
        ApiResponseSetupAddress(postcode = postCode1, fullAddress = null),
//        ApiResponseSetupAddress(postcode = postCode2, fullAddress = null),
//        ApiResponseSetupAddress(postcode = postCode3, fullAddress = null)
      ),
      sentences = listOf(
        ApiResponseSetupSentences(date1),
        ApiResponseSetupSentences(date2),
        ApiResponseSetupSentences(date3)
      )
    )
    probationDomainEventAndResponseSetup(NEW_OFFENDER_CREATED, deliusSetup)
    val deliusPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(deliusRecordCrn) })

    // create Libra Record
    val cId = randomCId()
    createRandomLibraPersonDetails(cId = cId)
    publishLibraMessage(libraHearing(cro = cro, firstName = firstName, cId = cId, lastName = lastName, postcode = postCode1, dateOfBirth = date1.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))))
    val libraPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCId(cId) })

    awaitAssert {
      val scores = personMatchClient.getPersonScores(libraPersonRecord.matchId.toString())
      println("libra record scores are - $scores")
    }


    // create CP record
    publishCommonPlatformMessage(
      commonPlatformHearing(
        listOf(
          CommonPlatformHearingSetup(
            firstName = firstName,
            pnc = pnc,
            cro = cro,
            lastName = lastName,
            defendantId = defendantId,
            address = CommonPlatformHearingSetupAddress(buildingName = "", buildingNumber = "", thoroughfareName = "", dependentLocality = "", postTown = "", postcode = postCode1),
            dateOfBirth = date1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
          )
        )
      ),
    )
    // assert 1 cluster with 3 records
    val cpPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByDefendantId(defendantId) })
//    cpPersonRecord.personKey!!.assertClusterIsOfSize(2)
    awaitAssert {
      val scores = personMatchClient.getPersonScores(cpPersonRecord.matchId.toString())
      println("cp record scores are - $scores")
    }
  }

  @Test
  fun `should create 2 people who do not match followed by a third which matches both and all end up on same cluster`() {
    val firstCrnWithPnc = randomCrn()
    val secondCrnWithCro = randomCrn()
    val thirdCrnWithBoth = randomCrn()
    val pnc = randomPnc()
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

    val firstPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(firstCrnWithPnc) })
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

    val secondPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(secondCrnWithCro) })
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

    val thirdPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(thirdCrnWithBoth) })
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

    val pnc = randomPnc()
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

    val firstPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(firstCrnWithPnc) })
    assertThat(firstPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(firstPersonRecord.getPnc()).isEqualTo(pnc)
    assertThat(firstPersonRecord.addresses.size).isEqualTo(1)
    assertThat(firstPersonRecord.personKey!!.personEntities.size).isEqualTo(1)

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

    val secondPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(secondCrnWithCro) })
    assertThat(secondPersonRecord.getPrimaryName().lastName).isEqualTo(basePerson.lastName)
    assertThat(secondPersonRecord.getCro()).isEqualTo(cro)
    assertThat(secondPersonRecord.personKey!!.personEntities.size).isEqualTo(2)
    assertThat(secondPersonRecord.personKey!!.personUUID).isEqualTo(firstPersonRecord.personKey!!.personUUID)
  }

  @Test
  fun `handle a new person which matches two clusters with override markers`() {
    val firstCrn = randomCrn()
    val secondCrn = randomCrn()
    val thirdCrn = randomCrn()

    val pnc = randomPnc()
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
    var firstPersonRecord = awaitNotNullPerson(timeout = 70, function = { personRepository.findByCrn(firstCrn) })
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
    var secondPersonRecord = awaitNotNullPerson(timeout = 70, function = { personRepository.findByCrn(secondCrn) })
    assertThat(secondPersonRecord.personKey!!.personEntities.size).isEqualTo(1)

    secondPersonRecord.assertExcludedFrom(firstPersonRecord)

    // create a new person which matches both
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
    val thirdPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(thirdCrn) })
    assertThat(thirdPersonRecord.personKey!!.personEntities.size).isEqualTo(1)

    assertThat(thirdPersonRecord.personKey!!.status).isEqualTo(NEEDS_ATTENTION_EXCLUDE)
    secondPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(secondCrn) })
    assertThat(secondPersonRecord.personKey!!.personEntities.size).isEqualTo(1)

    assertThat(secondPersonRecord.personKey!!.status).isEqualTo(NEEDS_ATTENTION_EXCLUDE)

    firstPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(firstCrn) })
    assertThat(firstPersonRecord.personKey!!.personEntities.size).isEqualTo(1)
    assertThat(firstPersonRecord.personKey!!.status).isEqualTo(NEEDS_ATTENTION_EXCLUDE)
  }
}
