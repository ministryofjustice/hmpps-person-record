package uk.gov.justice.digital.hmpps.personrecord.test.responses

fun prisonerSearchResponse(responseSetup: ApiResponseSetup) = """
  {
    "prisonerNumber": "${responseSetup.prisonNumber}",
    "pncNumber": "${responseSetup.pnc}",
    "pncNumberCanonicalShort": "${responseSetup.pnc}",
    "pncNumberCanonicalLong": "${responseSetup.pnc}",
    "croNumber": "${responseSetup.cro}",
    "bookingId": "0001200924",
    "bookNumber": "38412A",
    "title": "${responseSetup.title}",
    "firstName": "${responseSetup.firstName}",
    "middleNames": "${responseSetup.middleName} ${responseSetup.middleName}",
    "lastName": "${responseSetup.lastName}",
    "dateOfBirth": "${responseSetup.dateOfBirth}",
    "gender": ${responseSetup.gender?.let {""" "${responseSetup.gender}" """}},
    ${responseSetup.ethnicity?.let { """ "ethnicity": "${responseSetup.ethnicity}", """.trimIndent() } ?: "" }
    "youthOffender": true,
    "maritalStatus": "Widowed",
    ${responseSetup.religion?.let { """ "religion": "${responseSetup.religion}", """.trimIndent() } ?: "" }
    ${responseSetup.nationality?.let { """ "nationality": "${responseSetup.nationality}", """.trimIndent() } ?: "" }
    "status": "ACTIVE IN",
    "lastMovementTypeCode": "CRT",
    "lastMovementReasonCode": "CA",
    "inOutStatus": "IN",
    "prisonId": "MDI",
    "lastPrisonId": "MDI",
    "prisonName": "HMP Leeds",
    "cellLocation": "A-1-002",
    "aliases": [${responseSetup.aliases.joinToString { alias(it) }}],
    "alerts": [
      {
        "alertType": "H",
        "alertCode": "HA",
        "active": true,
        "expired": true
      }
    ],
    "csra": "HIGH",
    "category": "C",
    "legalStatus": "SENTENCED",
    "imprisonmentStatus": "LIFE",
    "imprisonmentStatusDescription": "Serving Life Imprisonment",
    "mostSeriousOffence": "Robbery",
    "recall": false,
    "indeterminateSentence": true,
    "sentenceStartDate": "2020-04-03",
    "releaseDate": "2023-05-02",
    "confirmedReleaseDate": "2023-05-01",
    "sentenceExpiryDate": "2023-05-01",
    "licenceExpiryDate": "2023-05-01",
    "homeDetentionCurfewEligibilityDate": "2023-05-01",
    "homeDetentionCurfewActualDate": "2023-05-01",
    "homeDetentionCurfewEndDate": "2023-05-02",
    "topupSupervisionStartDate": "2023-04-29",
    "topupSupervisionExpiryDate": "2023-05-01",
    "additionalDaysAwarded": 10,
    "nonDtoReleaseDate": "2023-05-01",
    "nonDtoReleaseDateType": "ARD",
    "receptionDate": "2023-05-01",
    "paroleEligibilityDate": "2023-05-01",
    "automaticReleaseDate": "2023-05-01",
    "postRecallReleaseDate": "2023-05-01",
    "conditionalReleaseDate": "2023-05-01",
    "actualParoleDate": "2023-05-01",
    "tariffDate": "2023-05-01",
    "releaseOnTemporaryLicenceDate": "2023-05-01",
    "locationDescription": "Outside - released from Leeds",
    "restrictedPatient": true,
    "supportingPrisonId": "LEI",
    "dischargedHospitalId": "HAZLWD",
    "dischargedHospitalDescription": "Hazelwood House",
    "dischargeDate": "2020-05-01",
    "dischargeDetails": "Psychiatric Hospital Discharge to Hazelwood House",
    "currentIncentive": {
      "level": {
        "code": "STD",
        "description": "Standard"
      },
      "dateTime": "2021-07-05T10:35:17",
      "nextReviewDate": "2022-11-10"
    },
    "heightCentimetres": 200,
    "weightKilograms": 102,
    "hairColour": "Blonde",
    "rightEyeColour": "Green",
    "leftEyeColour": "Hazel",
    "facialHair": "Clean Shaven",
    "shapeOfFace": "Round",
    "build": "Muscular",
    "shoeSize": 10,
    "tattoos": [
      {
        "bodyPart": "Head",
        "comment": "Skull and crossbones covering chest"
      }
    ],
    "scars": [
      {
        "bodyPart": "Head",
        "comment": "Skull and crossbones covering chest"
      }
    ],
    "marks": [
      {
        "bodyPart": "Head",
        "comment": "Skull and crossbones covering chest"
      }
    ],
    "addresses": [${responseSetup.addresses.joinToString { address(it) }}],
    "emailAddresses": [
        ${responseSetup.email?.let { """ {"email": "${responseSetup.email}" }""".trimIndent() } }
    ],
    "phoneNumbers": [
      {
        "type": "HOME, MOB",
        "number": "01141234567"
      }
    ],
    "identifiers": [${responseSetup.identifiers.joinToString { identifier(it) }}],
    "allConvictedOffences": [
      {
        ${responseSetup.sentenceStartDate?.let { """ "sentenceStartDate": "${responseSetup.sentenceStartDate}", """.trimIndent() } ?: ""}
        ${responseSetup.primarySentence?.let { """ "primarySentence": "${responseSetup.primarySentence}", """.trimIndent() } ?: ""}
        "statuteCode": "TH68",
        "offenceCode": "TH68010",
        "offenceDescription": "Theft from a shop",
        "offenceDate": "2024-05-23",
        "latestBooking": true
      }
    ]
  }
""".trimIndent()

private fun alias(alias: ApiResponseSetupAlias) =
  """
          {
            "title": "${alias.title ?: ""}",
            "firstName": "${alias.firstName ?: ""}",
            "middleNames": "${alias.middleName ?: ""}",
            "lastName": "${alias.lastName ?: ""}",
            "dateOfBirth": "${alias.dateOfBirth ?: ""}",
            "gender": "${alias.gender}",
            "ethnicity": "White : Irish"
          }
  """.trimIndent()

private fun identifier(identifier: ApiResponseSetupIdentifier) =
  """
    {
      "type": "${identifier.type}",
      "value": "${identifier.value}"
    }
  """.trimIndent()

private fun address(address: ApiResponseSetupAddress) =
  """
    {
      ${address.fullAddress?.let { """ "fullAddress": "${address.fullAddress}", """.trimIndent() } ?: "" }
      ${address.postcode?.let { """ "postalCode": "${address.postcode}", """.trimIndent() } ?: "" }
      ${address.startDate?.let { """ "startDate": "${address.startDate}", """.trimIndent() } ?: "" }
      ${address.noFixedAbode?.let { """ "noFixedAddress": "${address.noFixedAbode}", """.trimIndent() } ?: "" }
      "primaryAddress": true,
      "phoneNumbers": [
        {
          "type": "HOME, MOB",
          "number": "01141234567"
        }
      ]
    }
  """.trimIndent()
