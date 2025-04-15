package uk.gov.justice.digital.hmpps.personrecord.message.processors.court

import com.jayway.jsonpath.JsonPath
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

fun addCprUUIDToCommonPlatform(
  message: String,
  processedDefendants: List<PersonEntity>?,
): String {
  val messageParser = JsonPath.parse(message)
  processedDefendants?.forEach { defendant ->
    val defendantId = defendant.defendantId
    val cprUUID = defendant.personKey?.personId.toString()
    messageParser.put(
      "$.hearing.prosecutionCases[?(@.defendants[?(@.id == '$defendantId')])].defendants[?(@.id == '$defendantId')]",
      "cprUUID",
      cprUUID,
    )
  }
  return messageParser.jsonString()
}

fun addCprUUIDToLibra(
  message: String,
  defendant: PersonEntity?,
): String {
  val messageParser = JsonPath.parse(message)

  defendant?.personKey?.personId?.let {
    messageParser.put(
      "$",
      "cprUUID",
      it.toString(),
    )
  }

  return messageParser.jsonString()
}
