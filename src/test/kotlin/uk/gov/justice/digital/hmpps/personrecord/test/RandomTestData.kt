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

fun randomHearingId(): String = UUID.randomUUID().toString()

fun randomCId(): Long = randomDigit(9).toLong()

private fun randomLowerCaseString(length: Int = 7): String = (1..length).map {
  ('a' + (Math.random() * 26).toInt())
}.joinToString("")

private fun randomDigit(length: Int = 7): String = (1..length).map {
  (0..9).random()
}.joinToString("")

private fun randomYear() = (1950..LocalDate.now().year).random()
