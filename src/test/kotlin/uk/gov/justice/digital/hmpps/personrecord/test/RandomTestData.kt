package uk.gov.justice.digital.hmpps.personrecord.test

import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID

fun randomPnc(): String {
  val allPNCs = Files.readAllLines(Paths.get("src/test/resources/valid_pncs.csv"), Charsets.UTF_8)

  return allPNCs.get((0..allPNCs.size).random())
}

fun randomCro(): String {
  val allCROs = Files.readAllLines(Paths.get("src/test/resources/valid_cros.csv"), Charsets.UTF_8)

  return allCROs.get((0..allCROs.size).random())
}

fun randomFirstName(): String = UUID.randomUUID().toString()

fun randomLastName(): String = UUID.randomUUID().toString()
fun randomNationalInsuranceNumber(): String = UUID.randomUUID().toString()
fun randomDriverLicenseNumber(): String = UUID.randomUUID().toString()
