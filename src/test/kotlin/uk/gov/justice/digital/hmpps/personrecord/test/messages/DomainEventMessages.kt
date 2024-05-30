package uk.gov.justice.digital.hmpps.personrecord.test.messages

fun prisonerDomainEvent(eventType: String?, prisonNumber: String, messageId: String?) = """
  {
    "Type": "Notification",
    "MessageId": "$messageId",
    "TopicArn": "arn:aws:sns:eu-west-2:000000000000:0fad8016-1982-4a2a-bc90-f39934bd78ca",
    "Message": "{\"eventType\":\"$eventType\",\"detailUrl\":\"https://prisoner-search-dev.prison.service.justice.gov.uk/prisoner/$prisonNumber\",\"personReference\":null,\"additionalInformation\":{\"categoriesChanged\":[],\"nomsNumber\":\"$prisonNumber\"}}",
    "MessageAttributes": {
      "eventType": {
        "Type": "String",
        "Value": "$eventType"
      }
    },
    "SignatureVersion": "1"
  }   
""".trimIndent()

fun offenderDomainEvent(eventType: String?, crn: String, messageId: String?) = """
  {
    "Type": "Notification",
    "MessageId": "$messageId",
    "TopicArn": "arn:aws:sns:eu-west-2:000000000000:0fad8016-1982-4a2a-bc90-f39934bd78ca",
    "Message": "{\"eventType\":\"$eventType\",\"detailUrl\":\"https://domain-events-and-delius-dev.hmpps.service.justice.gov.uk/probation-case.engagement.created/$crn\",\"personReference\": {\"identifiers\": [ {\"type\": \"CRN\", \"value\": \"$crn\"} ]},\"additionalInformation\":{}}",
    "MessageAttributes": {
      "eventType": {
        "Type": "String",
        "Value": "$eventType"
      }
    },
    "SignatureVersion": "1"
  }   
""".trimIndent()
