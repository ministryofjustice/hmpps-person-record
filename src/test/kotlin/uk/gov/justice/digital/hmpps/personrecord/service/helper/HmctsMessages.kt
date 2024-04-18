package uk.gov.justice.digital.hmpps.personrecord.service.helper

import org.testcontainers.shaded.org.bouncycastle.asn1.isismtt.x509.DeclarationOfMajority.dateOfBirth

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

fun testMessageWithUnknownType(messageType: String?) = """
    {
      "Type" : "Unknown",
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

fun commonPlatformHearing(pncNumber: String = "1981/0154257C") = """
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
                "pncId": "$pncNumber",
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

fun commonPlatformHearingWithOneDefendant(pncNumber: String = "1981/0154257C", firstName: String = "Horace", lastName: String = "Andy", dateOfBirth: String = "1975-01-01", cro: String = "86621/65B", defendantId: String = "0ab7c3e5-eb4c-4e3f-b9e6-b9e78d3ea199") = """
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
                "id": "$defendantId",
                "pncId": "$pncNumber",
                "croNumber": "$cro",
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
                    "dateOfBirth": "$dateOfBirth",
                    "firstName": "$firstName",
                    "gender": "MALE",
                    "lastName": "$lastName",
                    "title": "Mr"
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

fun libraHearing(pncNumber: String? = "2003/0011985X", firstName: String = "Arthur", surname: String = "MORGAN", dateOfBirth: String = "01/01/1975") = """
{
   "caseId":1217464,
   "caseNo":"1600032981",
   "name":{
      "title":"Mr",
      "forename1":"$firstName",
      "surname":"$surname"
   },
   "defendantName":"Mr $firstName $surname",
   "defendantType":"P",
   "defendantSex":"N",
   "defendantDob":"$dateOfBirth",
   "defendantAge":"20",
   "defendantAddress":{
      "line1":"39 The Street",
      "line2":"Newtown",
      "pcode":"NT4 6YH"
   },
   "cro":"85227/65L",
   ${pncNumber?.let { """ "pnc": "$pncNumber", """.trimIndent() } ?: ""}
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

fun commonPlatformHearingWithNewDefendant(pncNumber: String = "2003/0062845E") = """
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
                "id": "b5cfae34-9256-43ad-87fb-ac3def34e2ac",
                "pncId": "$pncNumber",
                "croNumber": "51072/62R",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "dateOfBirth": "1960-01-01",
                    "firstName": "Eric",
                    "gender": "MALE",
                    "lastName": "Lassard",
                    "title": "Mr"
                  }
                },
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              },
              { 
                "id": "b5cfae34-9256-43ad-87fb-ac3def34e2ac",
                "pncId": "2003/0062845E",
                "croNumber": "75715/64Q",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "dateOfBirth": "1960-01-01",
                    "firstName": "Eric",
                    "gender": "MALE",
                    "lastName": "Lassard",
                    "title": "Mr"
                  }
                },
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              },
                           { 
                "id": "b5cfae34-9256-43ad-87fb-ac3def34e2ac",
                "pncId": "2003/0062845E",
                "croNumber": "20970/63D",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "dateOfBirth": "1960-01-01",
                    "firstName": "Eric",
                    "gender": "MALE",
                    "lastName": "Lassard",
                    "title": "Mr"
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

fun commonPlatformHearingWithAdditionalFields() = """
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
                "id": "b5cfae34-9256-43ad-87fb-ac3def34e2ac",
                "masterDefendantId": "eeb71c73-573b-444e-9dc3-4e5998d1be65",
                "pncId": "2003/0062845E",
                "croNumber": "33577/63G",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "dateOfBirth": "1960-01-01",
                    "firstName": "Eric",
                    "gender": "MALE",
                    "middleName": "mName1 mName2",
                    "lastName": "Lassard",
                    "title": "Mr"
                  }
                },
                "aliases": [ 
                    {
                     "firstName": "aliasFirstName1",
                     "lastName": "alisLastName1"
                    },
                    {
                     "firstName": "aliasFirstName2",
                     "lastName": "alisLastName2"
                    }
                  ],
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              },
              { 
                "id": "cc36c035-6e82-4d04-94c2-2a5728f11481",
                "masterDefendantId": "1f6847a2-6663-44dd-b945-fe2c20961d0a",
                "pncId": "2008/0056560Z",
                "croNumber": "78182/63Q",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "contact": {
                      "home": "0207345678",
                      "work": "0203788776",
                      "mobile": "078590345677",
                      "primaryEmail": "email@email.com"
                    },
                    "dateOfBirth": "1960-01-01",
                    "firstName": "CAREY",
                    "gender": "MALE",
                    "lastName": "Mahoney",
                    "title": "Mr"
                  }
                },
                "ethnicity": {
                   "observedEthnicityDescription": "observedEthnicityDescription",
                   "selfDefinedEthnicityDescription": "selfDefinedEthnicityDescription"
                },
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              },
                           { 
                "id": "b56f8612-0f4c-43e5-840a-8bedb17722ec",
                "masterDefendantId": "290e0457-1480-4e62-b3c8-7f29ef791c58",
                "pncId": "20230583843L",
                "croNumber": "15542/64K",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "15 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "dateOfBirth": "1964-01-01",
                    "firstName": "Leslie",
                    "gender": "MALE",
                    "lastName": "Barbara",
                    "title": "Mr",
                    "nationalityCode": "GB",
                    "nationalInsuranceNumber": "PC456743D"
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

fun commonPlatformHearingWithNewDefendantAndNoPnc() = """
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
                "id": "2d41e7b9-0964-48d8-8d2a-3f7e81b34cd7",
                "pncId": "",
                "croNumber": "51072/62R",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "dateOfBirth": "1960-01-01",
                    "firstName": "Eric",
                    "gender": "MALE",
                    "lastName": "Lassard",
                    "title": "Mr"
                  }
                },
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              },
              { 
                "id": "2d41e7b9-0964-48d8-8d2a-3f7e81b34cd7",
                "pncId": "",
                "croNumber": "75715/64Q",
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, John Smith",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  }
                ],
                "personDefendant": {
                  "personDetails": {
                    "address": {
                      "address1": "13 broad Street",
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    "dateOfBirth": "1960-01-01",
                    "firstName": "Eric",
                    "gender": "MALE",
                    "lastName": "Lassard",
                    "title": "Mr"
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
