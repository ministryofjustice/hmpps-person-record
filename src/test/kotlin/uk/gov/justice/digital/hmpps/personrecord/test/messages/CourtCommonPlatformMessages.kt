package uk.gov.justice.digital.hmpps.personrecord.test.messages

import uk.gov.justice.digital.hmpps.personrecord.test.randomFirstName
import java.util.UUID

data class CommonPlatformHearingSetup(val pnc: String? = null, val firstName: String? = randomFirstName(), val lastName: String = "Andy", val dateOfBirth: String = "1975-01-01", val cro: String = "86621/65B", val defendantId: String = UUID.randomUUID().toString())

fun commonPlatformHearing(commonPlatformHearingSetup: List<CommonPlatformHearingSetup>) = """
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
            "defendants": [${commonPlatformHearingSetup.map { defendant(it) }.joinToString(",")}],
            "id": "D2B61C8A-0684-4764-B401-F0A788BC7CCF",
            "prosecutionCaseIdentifier": {
              "caseURN": "25GD34377719"
            }
          }
        ]
      }
    }
""".trimIndent()

private fun defendant(commonPlatformHearingSetup: CommonPlatformHearingSetup) =
  """{ 
                "id": "${commonPlatformHearingSetup.defendantId}",
                "masterDefendantId": "${commonPlatformHearingSetup.defendantId}",
                "pncId": ${commonPlatformHearingSetup.pnc?.let { """ "${commonPlatformHearingSetup.pnc}" """.trimIndent() } ?: "null" },
                "croNumber": "${commonPlatformHearingSetup.cro}",
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
                    "dateOfBirth": "${commonPlatformHearingSetup.dateOfBirth}",
                    ${commonPlatformHearingSetup.firstName?.let { """ "firstName": "${commonPlatformHearingSetup.firstName}", """.trimIndent() } ?: ""}
                    "gender": "MALE",
                    "lastName": "${commonPlatformHearingSetup.lastName}",
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
              }
  """.trimIndent()

fun commonPlatformHearingWithAdditionalFields(defendantIds: List<String>) = """
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
                "id": "${defendantIds[0]}",
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
                "id": "${defendantIds[1]}",
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
                "id": "${defendantIds[2]}",
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
