package uk.gov.justice.digital.hmpps.personrecord.test

import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier.Companion.VALID_LETTERS
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.AdditionalIdentifierCode.Companion.probationAdditionalIdentifierCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.Companion.commonPlatformEthnicity
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.Companion.prisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode.Companion.probationEthnicity
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode.Companion.probationGenderIdentityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode.Companion.commonPlatformSexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode.Companion.libraSexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode.Companion.prisonSexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode.Companion.probationSexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation.Companion.prisonSexualOrientationMap
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation.Companion.probationSexualOrientationMap
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode.Companion.titleCodeMap
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.COMMON_PLATFORM_NATIONALITY_MAPPING
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.PRISON_NATIONALITY_MAPPING
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.PROBATION_NATIONALITY_MAPPING
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

fun randomLongPnc(): String {
  val year = randomYear().toString()
  val digits = randomDigit(7)
  val check = VALID_LETTERS[(year.takeLast(2) + digits).toInt().mod(VALID_LETTERS.length)]
  return "$year/$digits$check"
}

// output 79/123456H
fun randomShortPnc(): String {
  val year = randomYear().toString().takeLast(2)
  val digits = randomDigit(6)
  val check = VALID_LETTERS[(year + digits.padStart(7, '0')).toInt().mod(VALID_LETTERS.length)]
  return "$year/$digits$check"
}

fun randomPhoneNumber(): String = randomDigit(10)

fun randomEmail(): String = randomLowerCaseString(8) + "." + randomDigit(4) + "@" + randomLowerCaseString(8) + ".co.uk"

fun randomDate(): LocalDate = LocalDate.of(randomYear(), (1..12).random(), (1..28).random())

fun randomTime(): LocalTime = LocalTime.of((1..23).random(), (1..59).random(), (1..59).random())

fun randomDateTime(): LocalDateTime = LocalDateTime.of(randomDate(), randomTime())

fun randomCro(): String {
  val year = randomYear().toString().takeLast(2)
  val digits = randomDigit(6)
  val check = VALID_LETTERS[(year + digits).toInt().mod(VALID_LETTERS.length)]
  return "$digits/$year$check"
}
fun randomBuildingNumber(): String = randomDigit(3)

fun randomBoolean(): Boolean = listOf(true, false).random()

fun randomName(): String = randomLowerCaseString()

fun randomAddressNumber(): String = (1..200).random().toString()

fun randomUprn(): String = "2" + randomDigit(11)

fun randomTitleCode(): Map.Entry<String, TitleCode> = titleCodeMap.entries.random()

fun randomAdditionalIdentifierCode(): Map.Entry<String, AdditionalIdentifierCode> = probationAdditionalIdentifierCode.entries.random()

fun randomCommonPlatformSexCode(): Map.Entry<String, SexCode> = commonPlatformSexCode.entries.random()

fun randomLibraSexCode(): Map.Entry<String, SexCode> = libraSexCode.entries.random()

fun randomProbationSexCode(): Map.Entry<String, SexCode> = probationSexCode.entries.random()
fun randomProbationSexualOrientation(): Map.Entry<String, SexualOrientation> = probationSexualOrientationMap.entries.random()

fun randomProbationGenderIdentity(): Map.Entry<String, GenderIdentityCode> = probationGenderIdentityCode.entries.random()

fun randomPrisonSexualOrientation(): Map.Entry<String, SexualOrientation> = prisonSexualOrientationMap.entries.random()

fun randomDisability(): Boolean = listOf(true, false).random()

fun randomPrisonSexCode(): Map.Entry<String, SexCode> = prisonSexCode.entries.random()

fun randomProbationEthnicity(): String = probationEthnicity.keys.random()

fun randomCommonPlatformEthnicity(): String = commonPlatformEthnicity.keys.random()

fun randomPrisonEthnicity(): String = prisonEthnicity.keys.random()

fun randomNationalityCode(): NationalityCode = NationalityCode.entries.random()

fun randomPrisonNationalityCode(): String = PRISON_NATIONALITY_MAPPING.entries.random().key

fun randomProbationNationalityCode(): String = PROBATION_NATIONALITY_MAPPING.entries.random().key

fun randomCommonPlatformNationalityCode(): String = COMMON_PLATFORM_NATIONALITY_MAPPING.entries.random().key

fun randomReligion(): String = ReligionCode.entries.random().description

fun randomDriverLicenseNumber(): String {
  fun randomString(length: Int, source: String) = (1..length).map { source.random() }.joinToString("")
  val surnamePart = randomLowerCaseString(5).uppercase()
  val year = (50..99).random().toString() // Last two digits of birth year (1950-1999)
  val month = (1..12).random().toString().padStart(2, '0')
  val day = (1..31).random().toString().padStart(2, '0')
  val initials = randomLowerCaseString(2).uppercase()
  val checkDigits = randomString(2, randomDigit(2))
  return "$surnamePart$year$month$day$initials$checkDigits"
}

fun randomFullAddress(): String = randomDigit(2) + " " + randomLowerCaseString(8) + ", " + randomLowerCaseString(10) + ", " + randomPostcode()

fun randomPrisonNumber(): String = randomLowerCaseString(2).uppercase() + randomDigit(4) + randomLowerCaseString(1).uppercase()

fun randomDefendantId(): String = UUID.randomUUID().toString()

fun randomCrn(): String = randomLowerCaseString(1).uppercase() + randomDigit(6)

fun randomNationalInsuranceNumber(): String = randomLowerCaseString(2).uppercase() + randomDigit(6) + randomLowerCaseString(1).uppercase()

fun randomPostcode(): String = randomLowerCaseString(2).uppercase() + randomDigit(1) + " " + randomDigit(1) + randomLowerCaseString(2).uppercase()

fun randomArrestSummonNumber(): String = randomDigit(19) + randomLowerCaseString(1).uppercase()

fun randomHearingId(): String = UUID.randomUUID().toString()

fun randomCId(): String = randomDigit(9)

private fun randomLowerCaseString(length: Int = 7): String = (1..length).map {
  ('a' + (Math.random() * 26).toInt())
}.joinToString("")

private fun randomDigit(length: Int = 7): String = (1..length).map {
  (0..9).random()
}.joinToString("")

private fun randomYear() = (1950..LocalDate.now().year).random()
