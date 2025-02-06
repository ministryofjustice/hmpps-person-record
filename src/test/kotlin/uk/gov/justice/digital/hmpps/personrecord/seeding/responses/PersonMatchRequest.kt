package uk.gov.justice.digital.hmpps.personrecord.seeding.responses

import uk.gov.justice.digital.hmpps.personrecord.client.model.match.PersonMatchRecord

fun personMatchRequest(personMatchRecord: PersonMatchRecord): String = """
  {
    "records": [
      {
        "matchID": "${personMatchRecord.matchId}",
        "sourceSystem": "${personMatchRecord.sourceSystem}",
        "firstName": "${personMatchRecord.firstName}",
        "middleNames": "${personMatchRecord.middleNames}",
        "lastName": "${personMatchRecord.middleNames}",
        "dateOfBirth": "${personMatchRecord.dateOfBirth}",
        "firstNameAliases": ["${personMatchRecord.firstNameAliases}"], TODO: SORT THIS OUT
      }
    ]
 }
""".trimIndent()
