package uk.gov.justice.digital.hmpps.personrecord.test.messages

import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomHearingId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalInsuranceNumber

data class CommonPlatformHearingSetup(
  val pnc: String? = null,
  val firstName: String? = randomName(),
  val middleName: String? = null,
  val lastName: String = randomName(),
  val title: String? = null,
  val ethnicity: String? = null,
  val dateOfBirth: String = randomDate().toString(),
  val cro: String = randomCro(),
  val defendantId: String = randomDefendantId(),
  val aliases: List<CommonPlatformHearingSetupAlias>? = null,
  val contact: CommonPlatformHearingSetupContact? = null,
  val nationalityCode: String? = null,
  val nationalInsuranceNumber: String = randomNationalInsuranceNumber(),
  val hearingId: String = randomHearingId(),
  val isYouth: Boolean? = false,
  val isPerson: Boolean = true,
  val address: CommonPlatformHearingSetupAddress? = null,
  val gender: String? = "MALE",
  val pncMissing: Boolean = false,
  val croMissing: Boolean = false,
  val isYouthMissing: Boolean = false,
)

data class CommonPlatformHearingSetupAlias(val firstName: String, val lastName: String)
data class CommonPlatformHearingSetupContact(
  val home: String = "0207345678",
  val work: String = "0203788776",
  val mobile: String = "078590345677",
  val primaryEmail: String = "email@email.com",
)

data class CommonPlatformHearingSetupAddress(val buildingName: String, val buildingNumber: String, val thoroughfareName: String, val dependentLocality: String, val postTown: String, val postcode: String)

fun largeCommonPlatformMessage(s3Key: String, s3BucketName: String) = """
  ["software.amazon.payloadoffloading.PayloadS3Pointer",{"s3BucketName":"$s3BucketName","s3Key":"$s3Key"}]
"""

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
        "id": "${commonPlatformHearingSetup[0].hearingId}",
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

private fun defendant(commonPlatformHearingSetup: CommonPlatformHearingSetup) = """{ 
                "id": "${commonPlatformHearingSetup.defendantId}",
                "masterDefendantId": "${commonPlatformHearingSetup.defendantId}",
                ${pncId(commonPlatformHearingSetup)}
                ${croNumber(commonPlatformHearingSetup)}
                ${isYouth(commonPlatformHearingSetup)}
                "offences": [
                  {
                    "id": "a63d9020-aa6b-4997-92fd-72a692b036de",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, REDACTED",
                    "listingNumber": 30,
                    "offenceCode": "ABC001"
                  },
                  {
                    "id": "ea1c2cf1-f155-483b-a908-81158a9b2f9b",
                    "offenceLegislation": "Contrary to section 20 of the Offences Against the    Person Act 1861.",
                    "offenceTitle": "Wound / inflict grievous bodily harm without intent",
                    "wording": "on 01/08/2009 at  the County public house, unlawfully and maliciously wounded, REDACTED",
                    "listingNumber": 20,
                    "offenceCode": "ABC002"
                  }
                ],
                ${commonPlatformHearingSetup.isPerson.takeIf { it }?.let { personDefendant(commonPlatformHearingSetup)} ?: organisationDefendant(commonPlatformHearingSetup)}
                ${commonPlatformHearingSetup.aliases?.let {
  """ "aliases": [${commonPlatformHearingSetup.aliases.joinToString(",") { alias(it) }
  }], """.trimIndent()
} ?: ""}
                "prosecutionCaseId": "D2B61C8A-0684-4764-B401-F0A788BC7CCF"
              }
""".trimIndent()

private fun croNumber(commonPlatformHearingSetup: CommonPlatformHearingSetup) = when (commonPlatformHearingSetup.croMissing) {
  false -> """ "croNumber": "${commonPlatformHearingSetup.cro}", "isCroMissing": false,"""
  else -> """ "isCroMissing": true,"""
}

private fun pncId(commonPlatformHearingSetup: CommonPlatformHearingSetup) = when (commonPlatformHearingSetup.pncMissing) {
  false -> """ "pncId": ${commonPlatformHearingSetup.pnc?.let { """ "${commonPlatformHearingSetup.pnc}" """.trimIndent() } ?: "null"},"isPncMissing": false,""".trimIndent()
  else -> """ "isPncMissing": true,"""
}

private fun isYouth(commonPlatformHearingSetup: CommonPlatformHearingSetup) = when (commonPlatformHearingSetup.isYouthMissing) {
  false -> """ "isYouth": ${commonPlatformHearingSetup.isYouth?.let { """ "${commonPlatformHearingSetup.isYouth}" """.trimIndent() } ?: "null"},"isYouthMissing": false, """.trimIndent()
  else -> """ isYouthMissing": true,"""
}

private fun personDefendant(commonPlatformHearingSetup: CommonPlatformHearingSetup) = """
  "personDefendant": {
    "personDetails": {
      "address": {
        "address1": "${commonPlatformHearingSetup.address?.buildingName}",
        "address2": "${commonPlatformHearingSetup.address?.buildingNumber}",
        "address3": "${commonPlatformHearingSetup.address?.thoroughfareName}",
        "address4": "${commonPlatformHearingSetup.address?.dependentLocality}",
        "address5": "${commonPlatformHearingSetup.address?.postTown}",
        "postcode": "${commonPlatformHearingSetup.address?.postcode}"
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
      ${commonPlatformHearingSetup.ethnicity?.let {
  """
        "ethnicity": {
        "selfDefinedEthnicityCode": "${commonPlatformHearingSetup.ethnicity}"
         }, 
  """.trimIndent()
} ?: ""} 
      "dateOfBirth": "${commonPlatformHearingSetup.dateOfBirth}",
      ${commonPlatformHearingSetup.firstName?.let { """ "firstName": "${commonPlatformHearingSetup.firstName}", """.trimIndent() } ?: ""}
      "gender": "${commonPlatformHearingSetup.gender}",
      ${commonPlatformHearingSetup.middleName?.let { """ "middleName": "${commonPlatformHearingSetup.middleName}", """.trimIndent() } ?: ""}
      "lastName": "${commonPlatformHearingSetup.lastName}",
      "title": "${commonPlatformHearingSetup.title}",
      "nationalityCode": "${commonPlatformHearingSetup.nationalityCode ?: ""}",
      "nationalInsuranceNumber": "${commonPlatformHearingSetup.nationalInsuranceNumber}"
    }
  },
""".trimIndent()

private fun organisationDefendant(commonPlatformHearingSetup: CommonPlatformHearingSetup) = """
  "legalEntityDefendant": {
    "organisation": {
      "name": "${commonPlatformHearingSetup.firstName} ${commonPlatformHearingSetup.lastName}",
      "address": {
        "address1": "13 Wind Street",
        "address2": "Cardiff",
        "address3": "Wales",
        "address4": "UK",
        "address5": "Earth",
        "postcode": "CF10 1FU"
      }
    }
  },
""".trimIndent()

private fun alias(alias: CommonPlatformHearingSetupAlias) =
  """
  {
    "firstName": "${alias.firstName}",
    "lastName": "${alias.lastName}"
  }
  """.trimIndent()
