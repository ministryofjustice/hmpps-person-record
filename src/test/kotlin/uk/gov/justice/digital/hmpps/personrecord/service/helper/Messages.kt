package uk.gov.justice.digital.hmpps.personrecord.service.helper

fun testMessage(messageType: String?) = """
    {
      "Type" : "Notification",
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

fun commonPlatformHearing() = """
    {
      "hearing": {
        "type": {
          "description": "sentence"
        },
        "courtCentre": {
          "id": "9b583616-049b-30f9-a14f-028a53b7cfe8",
          "roomId": "7cb09222-49e1-3622-a5a6-ad253d2b3c39",
          "roomName": "Crown Court 3-1",
          "code": "B10JQ00"
        },
        "hearingDays": [
          {
            "listedDurationMinutes": 60,
            "listingSequence": 0,
            "sittingDay": "2021-09-08T09:00:00.000Z"
          },
          {
            "listedDurationMinutes": 30,
            "listingSequence": 1,
            "sittingDay": "2021-09-09T10:30:00.000Z"
          }
        ],
        "id": "E10E3EF3-8637-40E3-BDED-8ED104A380AC",
        "jurisdictionType": "CROWN",
        "prosecutionCases": [
          {
            "defendants": [
              { 
                "id": "0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199",
                "pncId": "1981/0154257C",
                "croNumber": "12345ABCDEF",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  },
                  {
                    "id": "ea1c2cf1-f155-483b-a908-81158a9b2f9b",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, Jane Smith",
                    "listingNumber": 20,
                    "offenceCode": "ABC002"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 Wind Street",
                      "address2": "Swansea",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "SA1 1FU"
                    },
                    "dateOfBirth": "1975-01-01",
                    "firstName": "Arthur",
                    "gender": "MALE",
                    "lastName": "MORGAN",
                    "title": "Mr"
                  }
                },
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              },
              {
                "id": "a3e8f57a-900f-4057-ab2c-ebe6887f98e1",
                "pncId": "2008/0056560Z",
                "croNumber": "12345ABCDEF",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  },
                  {
                    "id": "ea1c2cf1-f155-483b-a908-81158a9b2f9b",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, Jane Smith",
                    "listingNumber": 20,
                    "offenceCode": "ABC002"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 Wind Street",
                      "address2": "Swansea",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "SA1 1FU"
                    },
                    "dateOfBirth": "1976-01-01",
                    "firstName": "Harry",
                    "gender": "MALE",
                    "lastName": "Potter",
                    "title": "Mr"
                  }
                },
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              },
              {
                "id": "903c4c54-f667-4770-8fdf-1adbb5957c25",
                "offences": [
                  {
                    "id": "1391ADC2-7A43-48DC-8523-3D28B9DCD2B7",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "offenceCode": "ABC001"
                  },
                  {
                    "id": "19C08FB0-363B-4EB1-938D-76EF751E5D66",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, Jane Smith",
                    "offenceCode": "ABC002"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "14 Tottenham Court Road",
                      "address2": "London Road",
                      "address3": "England",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "W1T 7RJ"
                    },
                    "dateOfBirth": "1997-02-28",
                    "firstName": "Phyllis",
                    "middleName": "Ulon",
                    "gender": "FEMALE",
                    "lastName": "Leffler"
                  }
                },
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              }
            ],
            "id": "D2B61C8A-0684-4764-B401-F0A788BC7CCF",
            "prosecutionCaseIdentifier": {
              "caseURN": "25GD34377719"
            }
          }
        ]
      }
    }
""".trimIndent()

fun libraHearing() = """
{
   "caseId":1217464,
   "caseNo":"1600032981",
   "name":{
      "title":"Mr",
      "forename1":"Arthur",
      "surname":"MORGAN"
   },
   "defendantName":"Mr Arthur MORGAN",
   "defendantType":"P",
   "defendantSex":"N",
   "defendantDob":"01/01/1975",
   "defendantAge":"20",
   "defendantAddress":{
      "line1":"39 The Street",
      "line2":"Newtown",
      "pcode":"NT4 6YH"
   },
   "cro":"11111/79J",
   "pnc":"1923[1234567A",
   "listNo":"1st",
   "nationality1":"Angolan",
   "nationality2":"Austrian",
   "offences":[
      {
         "seq":1,
         "summary":"On 01/01/2016 at Town, stole Article, to the value of £100.00, belonging to Person.",
         "title":"Theft from a shop",
         "act":"Contrary to section 1(1) and 7 of the Theft Act 1968."
      }
   ],
   "sessionStartTime":"2020-02-20T09:01:00",
   "courtCode":"B10JQ",
   "courtRoom":"00",
   "seq":1
}
""".trimIndent()
