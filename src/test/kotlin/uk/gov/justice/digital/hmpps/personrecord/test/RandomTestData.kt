package uk.gov.justice.digital.hmpps.personrecord.test

import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier.Companion.VALID_LETTERS
import java.time.LocalDate
import java.util.UUID

fun randomPnc(): String {
  val year = randomYear().toString()
  val digits = randomDigit(7)
  val check = VALID_LETTERS[(year.takeLast(2) + digits).toInt().mod(VALID_LETTERS.length)]
  return "$year/$digits$check"
}

fun randomEmail(): String = randomLowerCaseString(8) + "." + randomDigit(4) + "@" + randomLowerCaseString(8) + ".co.uk"

fun randomDate(): LocalDate = LocalDate.of(randomYear(), (1..12).random(), (1..28).random())

fun randomCro(): String {
  val year = randomYear().toString().takeLast(2)
  val digits = randomDigit(6)
  val check = VALID_LETTERS[(year + digits).toInt().mod(VALID_LETTERS.length)]
  return "$digits/$year$check"
}

fun randomName(): String = randomLowerCaseString()

fun randomEthnicity(): String = randomLowerCaseString()

fun randomNationality(): String = randomLowerCaseString()

fun randomReligion(): String = randomLowerCaseString()

fun randomDriverLicenseNumber(): String = randomLowerCaseString(5).uppercase() + randomDigit(6) + randomLowerCaseString(5).uppercase()

fun randomFullAddress(): String = randomDigit(2) + " " + randomLowerCaseString(8) + ", " + randomLowerCaseString(10) + ", " + randomPostcode()

fun randomPrisonNumber(): String = randomLowerCaseString(2).uppercase() + randomDigit(4) + randomLowerCaseString(1).uppercase()

fun randomDefendantId(): String = UUID.randomUUID().toString()

fun randomCrn(): String = randomLowerCaseString(1).uppercase() + randomDigit(6)

fun randomNationalInsuranceNumber(): String = randomLowerCaseString(2).uppercase() + randomDigit(6) + randomLowerCaseString(1).uppercase()

fun randomPostcode(): String = randomLowerCaseString(2).uppercase() + randomDigit(1) + " " + randomDigit(1) + randomLowerCaseString(2).uppercase()

fun randomHearingId(): String = UUID.randomUUID().toString()

private fun randomLowerCaseString(length: Int = 7): String = (1..length).map {
  ('a' + (Math.random() * 26).toInt())
}.joinToString("")

private fun randomDigit(length: Int = 7): String = (1..length).map {
  (0..9).random()
}.joinToString("")

private fun randomYear() = (1950..LocalDate.now().year).random()
