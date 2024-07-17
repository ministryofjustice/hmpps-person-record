package uk.gov.justice.digital.hmpps.personrecord.test.messages

fun probationEventMessage(crn: String) = """
  {
    "offenderId": 1502319560,
    "crn": "$crn",
    "sourceId": 1502319560,
    "eventDatetime": "2024-07-16T14:29:07+01:00"
  }
""".trimIndent()
