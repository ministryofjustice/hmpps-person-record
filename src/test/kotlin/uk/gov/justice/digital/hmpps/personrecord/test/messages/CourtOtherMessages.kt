package uk.gov.justice.digital.hmpps.personrecord.test.messages

fun testMessage(messageType: String?, type: String = "Notification") = """
    {
      "Type" : "$type",
      "MessageId" : "5bc08be0-16e9-5da9-b9ec-d2c870a59bad",
      "Message" : "{  \"caseId\": 1217464, \"hearingId\": \"hearing-id-one\",   \"caseNo\": \"1600032981\"}}",
      "MessageAttributes": {
          "messageType": {
            "Type": "String",
            "Value": "$messageType"
          },
          "hearingEventType": {
            "Type": "String",
            "Value": "ConfirmedOrUpdated"
          }
      }
     }    
""".trimIndent()
