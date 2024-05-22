package uk.gov.justice.digital.hmpps.personrecord.service.helper

fun notFoundResponse(crn: String) = """
  {
    "status": 404,
    "developerMessage": "Not found"
  }
""".trimIndent()

fun emptyPnc(crn: String) = """
  {
    "identifiers": {
      "crn": "$crn" },
    "name": {
      "forename": "Martin",
      "surname": "Bundle",
      "otherNames": []
    },
    "dateOfBirth": "1979-10-10"
  }
""".trimIndent()

fun nullPnc(crn: String) = """
  {
    "identifiers": {
      "crn": "$crn",
    "pnc": null  },
    "name": {
      "forename": "Martin",
      "surname": "Bundle",
      "otherNames": []
    },
    "dateOfBirth": "1979-10-10"
  }
""".trimIndent()

fun missingPnc(crn: String) = """
  {
    "identifiers": {
      "crn": "$crn"
    },
    "name": {
      "forename": "David",
      "surname": "BOWIE",
      "otherNames": []
    },
    "dateOfBirth": "1939-10-10"
  }
""".trimIndent()

fun newProbationRecord(crn: String, pnc: String = "2020/0476873U") = """
  {
    "identifiers": {
      "crn": "$crn",
      "pnc": "$pnc"
    },
    "name": {
      "forename": "David",
      "surname": "BOWIE",
      "otherNames": []
    },
    "dateOfBirth": "1939-10-10"
  }
""".trimIndent()
