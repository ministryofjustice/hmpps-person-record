package uk.gov.justice.digital.hmpps.personrecord.test

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import kotlin.text.Charsets.UTF_8

fun randomPnc(): String {
  val allPNCs = Files.readAllLines(Paths.get("src/test/resources/valid_pncs.csv"), UTF_8)
  return allPNCs.get((0..allPNCs.size).random())
}

fun randomCro(): String {
  val allCROs = Files.readAllLines(Paths.get("src/test/resources/valid_cros.csv"), UTF_8)
  return allCROs.get((0..allCROs.size).random())
}

fun randomFirstName(): String = randomLowerCaseString()
fun randomLastName(): String = randomLowerCaseString()
fun randomNationalInsuranceNumber(): String = UUID.randomUUID().toString()
fun randomDriverLicenseNumber(): String = UUID.randomUUID().toString()

fun randomPrisonNumber(): String = randomLowerCaseString(2).uppercase() + randomDigit(4) + randomLowerCaseString(1).uppercase()

fun randomCRN(): String = randomLowerCaseString(1).uppercase() + randomDigit(6)

private fun randomLowerCaseString(length: Int = 7): String = (1..length).map {
  ('a' + (Math.random() * 26).toInt())
}.joinToString("")

private fun randomDigit(length: Int = 7): String = (1..length).map {
  (0 + (Math.random() * 9).toInt())
}.joinToString("")
