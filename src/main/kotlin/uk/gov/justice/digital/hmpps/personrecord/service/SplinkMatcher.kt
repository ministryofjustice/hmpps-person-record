package uk.gov.justice.digital.hmpps.personrecord.service

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.Person

class SplinkMatcher {

  private fun testData(): String = """[
  {
    "source_dataset": "cpr",
    "unique_id": "1",
    "dob_std": "01/10/2010",
    "forename1_std": "One",
    "forename2_std": "Two",
    "forename3_std": "Three",
    "surname_std": "Four",
    "pnc_number_std": "2020/0001234L"
  },
  {
    "source_dataset": "delius",
    "unique_id": "2",
    "dob_std": "01/10/2010",
    "forename1_std": "One",
    "forename2_std": "Two",
    "forename3_std": "Three",
    "surname_std": "Five",
    "pnc_number_std": "2020/0001234L"
  }
  ]"""

  fun matchScore(person: Person, defendant: DefendantEntity): String {
    print(person.givenName)
    print(defendant.surname)
    val process = ProcessBuilder("python", "scripts/match.py", testData()).start()
    val exitCode = process.waitFor()
    if (exitCode == 0) {
      return process.inputStream.bufferedReader().readLines().joinToString()
    }
    throw RuntimeException("It has gone wrong")
  }
}
