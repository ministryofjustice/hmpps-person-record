package uk.gov.justice.digital.hmpps.personrecord.test

import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier.Companion.VALID_LETTERS
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import java.time.LocalDate
import java.util.UUID

fun randomPnc(): String {
  val year = (1950..LocalDate.now().year).random().toString()
  val digits = randomDigit(7)
  val check = VALID_LETTERS[(year.takeLast(2) + digits).toInt().mod(VALID_LETTERS.length)]
  if (PNCIdentifier.from("$year/$digits$check").valid) {
    return "$year/$digits$check"
  }
  throw Exception("$year/$digits$check")
}

fun randomEmail(): String =
  randomLowerCaseString(8) + "." + randomDigit(4) + "@" + randomLowerCaseString(8) + ".co.uk"

fun randomDate(): LocalDate = LocalDate.of((1950..LocalDate.now().year).random(), (1..12).random(), (1..28).random())

fun randomCro(): String {
  val year = (1950..LocalDate.now().year).random().toString().takeLast(2)
  val digits = randomDigit(6)

  val check = VALID_LETTERS[(year + digits).toInt().mod(VALID_LETTERS.length)]
  if (CROIdentifier.from("$digits/$year$check").valid) {
    return "$digits/$year$check"
  }
  throw Exception(CROIdentifier.from("$digits/$year$check").croId)
}

fun randomName(): String = randomLowerCaseString()

fun randomEthnicity(): String = randomLowerCaseString()
fun randomNationality(): String = randomLowerCaseString()
fun randomReligion(): String = randomLowerCaseString()

fun randomNationalInsuranceNumber(): String = UUID.randomUUID().toString()
fun randomDriverLicenseNumber(): String = UUID.randomUUID().toString()

fun randomFullAddress(): String = randomDigit(2) + " " + randomLowerCaseString(8) + ", " + randomLowerCaseString(10) + ", " + randomPostcode()

fun randomPrisonNumber(): String = randomLowerCaseString(2).uppercase() + randomDigit(4) + randomLowerCaseString(1).uppercase()

fun randomDefendantId(): String = UUID.randomUUID().toString()

fun randomCRN(): String = randomLowerCaseString(1).uppercase() + randomDigit(6)
fun randomNINumber(): String = randomLowerCaseString(2).uppercase() + randomDigit(6) + randomLowerCaseString(1).uppercase()

fun randomPostcode(): String = randomLowerCaseString(2).uppercase() + randomDigit(1) + " " + randomDigit(1) + randomLowerCaseString(2).uppercase()

fun randomHearingId(): String = UUID.randomUUID().toString()
private fun randomLowerCaseString(length: Int = 7): String = (1..length).map {
  ('a' + (Math.random() * 26).toInt())
}.joinToString("")

private fun randomDigit(length: Int = 7): String = (1..length).map {
  (0..9).random()
}.joinToString("")
