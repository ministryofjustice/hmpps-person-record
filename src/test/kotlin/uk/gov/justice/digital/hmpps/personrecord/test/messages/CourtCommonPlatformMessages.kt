package uk.gov.justice.digital.hmpps.personrecord.test.messages

import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber
import java.util.UUID

data class CommonPlatformHearingSetup(
  val pnc: String? = null,
  val firstName: String? = randomName(),
  val middleName: String? = null,
  val lastName: String = randomName(),
  val dateOfBirth: String = "1975-01-01",
  val cro: String = randomCro(),
  val defendantId: String = UUID.randomUUID().toString(),
  val aliases: List<CommonPlatformHearingSetupAlias>? = null,
  val contact: CommonPlatformHearingSetupContact? = null,
  val nationalInsuranceNumber: String = randomNationalInsuranceNumber(),
)
data class CommonPlatformHearingSetupAlias(val firstName: String, val lastName: String)
data class CommonPlatformHearingSetupContact(
  val home: String = "0207345678",
  val work: String = "0203788776",
  val mobile: String = "078590345677",
  val primaryEmail: String = "email@email.com",
)
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
            "defendants": [${commonPlatformHearingSetup.joinToString(",") { defendant(it) }}],
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
                      "address2": "Cardiff",
                      "address3": "Wales",
                      "address4": "UK",
                      "address5": "Earth",
                      "postcode": "CF10 1FU"
                    },
                    ${commonPlatformHearingSetup.contact?.let {
    """
                      "contact": {
                       "home": "${commonPlatformHearingSetup.contact.home}",
                        "work": "${commonPlatformHearingSetup.contact.work}",
                        "mobile": "${commonPlatformHearingSetup.contact.mobile}",
                        "primaryEmail": "${commonPlatformHearingSetup.contact.primaryEmail}"
                       }, 
    """.trimIndent()
  } ?: ""}   
                    "dateOfBirth": "${commonPlatformHearingSetup.dateOfBirth}",
                    ${commonPlatformHearingSetup.firstName?.let { """ "firstName": "${commonPlatformHearingSetup.firstName}", """.trimIndent() } ?: ""}
                    "gender": "MALE",
                    ${commonPlatformHearingSetup.middleName?.let { """ "middleName": "${commonPlatformHearingSetup.middleName}", """.trimIndent() } ?: ""}
                    "lastName": "${commonPlatformHearingSetup.lastName}",
                    "title": "Mr",
                    "nationalityCode": "GB",
                    "nationalInsuranceNumber": "${commonPlatformHearingSetup.nationalInsuranceNumber}"
                  }
                },
                "ethnicity": {
                   "observedEthnicityDescription": "observedEthnicityDescription",
                   "selfDefinedEthnicityDescription": "selfDefinedEthnicityDescription"
                },
                ${commonPlatformHearingSetup.aliases?.let {
    """ "pseudonyms": [${commonPlatformHearingSetup.aliases.joinToString(",") { alias(it) }
    }], """.trimIndent()
  } ?: ""}
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              }
  """.trimIndent()

private fun alias(alias: CommonPlatformHearingSetupAlias) =
  """
  {
    "firstName": "${alias.firstName}",
    "lastName": "${alias.lastName}"
  }
  """.trimIndent()
