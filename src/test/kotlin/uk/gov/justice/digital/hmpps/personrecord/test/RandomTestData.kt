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

fun randomFirstName(): String = randomString()
fun randomLastName(): String = randomString()
fun randomNationalInsuranceNumber(): String = UUID.randomUUID().toString()
fun randomDriverLicenseNumber(): String = UUID.randomUUID().toString()

private fun randomString(): String = (1..(6 + (Math.random() * 7).toInt())).map {
  ('a' + (Math.random() * 26).toInt())
}.joinToString("")
