package uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.commonplatform

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.MessagingTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType.NEEDS_ATTENTION_EXCLUDE
import uk.gov.justice.digital.hmpps.personrecord.service.type.NEW_OFFENDER_CREATED
import uk.gov.justice.digital.hmpps.personrecord.service.type.OFFENDER_UNMERGED
import uk.gov.justice.digital.hmpps.personrecord.service.type.TelemetryEventType.CPR_RECORD_CREATED
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomFullAddress
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetup
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAddress
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupAlias
import uk.gov.justice.digital.hmpps.personrecord.test.responses.ApiResponseSetupSentences

@ActiveProfiles("e2e")
class JoinClustersE2ETest : MessagingTestBase() {

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
    assertThat(firstPersonRecord.personKey!!.personEntities.size).isEqualTo(1)

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
    assertThat(secondPersonRecord.personKey!!.personEntities.size).isEqualTo(1)
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
    assertThat(thirdPersonRecord.personKey!!.personEntities.size).isEqualTo(3)
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
    assertThat(secondPersonRecord.personKey!!.status).isEqualTo(NEEDS_ATTENTION_EXCLUDE)

    firstPersonRecord = awaitNotNullPerson(timeout = 7, function = { personRepository.findByCrn(firstCrn) })
    assertThat(firstPersonRecord.personKey!!.status).isEqualTo(NEEDS_ATTENTION_EXCLUDE)
  }
}
