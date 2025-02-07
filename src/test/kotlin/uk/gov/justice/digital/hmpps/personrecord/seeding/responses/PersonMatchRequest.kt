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
        "lastName": "${personMatchRecord.lastName}",
        "dateOfBirth": "${personMatchRecord.dateOfBirth}",
        "firstNameAliases": [${personMatchRecord.firstNameAliases.joinToString(", ") { "\"${it}\"" }}],
        "lastNameAliases": [${personMatchRecord.lastNameAliases.joinToString(", ") { "\"${it}\"" }}],
        "dateOfBirthAliases": [${personMatchRecord.dateOfBirthAliases.joinToString(", ") { "\"${it}\"" }}],
        "postcodes": [${personMatchRecord.postcodes.joinToString(", ") { "\"${it}\"" }}],
        "cros": [${personMatchRecord.cros.joinToString(", ") { "\"${it}\"" }}],
        "pncs": [${personMatchRecord.pncs.joinToString(", ") { "\"${it}\"" }}],     
        "sentenceDates": [${personMatchRecord.sentenceDates.joinToString(", ") { "\"${it}\"" }}]
      }
    ]
 }
""".trimIndent()
