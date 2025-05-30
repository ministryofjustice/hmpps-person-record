package uk.gov.justice.digital.hmpps.personrecord.seeding.responses

import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc

fun onePrisoner(prisonNumber: String, prefix: String) = """
  [{
    "prisonerNumber": "$prisonNumber",
    "pncNumber": "${randomPnc()}",
    "pncNumberCanonicalShort": "",
    "pncNumberCanonicalLong": "${randomPnc()}",
    "croNumber": "",
    "bookingId": "0001200924",
    "bookNumber": "38412A",
    "firstName": "${prefix}FirstName",
    "middleNames": "",
    "lastName": "${prefix}LastName",
    "dateOfBirth": "1975-04-02",
    "gender": "Female",
    "ethnicity": "White: Eng./Welsh/Scot./N.Irish/British",
    "youthOffender": true,
    "maritalStatus": "Widowed",
    "religion": "Church of England (Anglican)",
    "nationality": "Egyptian",
    "status": "ACTIVE IN",
    "lastMovementTypeCode": "CRT",
    "lastMovementReasonCode": "CA",
    "inOutStatus": "IN",
    "prisonId": "MDI",
    "lastPrisonId": "MDI",
    "prisonName": "HMP Leeds",
    "cellLocation": "A-1-002",
    "aliases": [
      {
        "firstName": "${prefix}AliasOneFirstName",
        "middleNames": "${prefix}AliasOneMiddleNameOne ${prefix}AliasOneMiddleNameTwo",
        "lastName": "${prefix}AliasOneLastName",
        "dateOfBirth": "1975-04-02",
        "gender": "Male",
        "ethnicity": "White : Irish"
      },
  {
        "firstName": "${prefix}AliasTwoFirstName",
        "middleNames": "${prefix}AliasTwoMiddleNameOne ${prefix}AliasTwoMiddleNameTwo",
        "lastName": "${prefix}AliasTwoLastName",
        "dateOfBirth": "1975-04-02",
        "gender": "Male",
        "ethnicity": "White : Irish"
      }
    ],
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
    ]
  }
  ]
  
""".trimIndent()

fun twoPrisoners(prisonNumberOne: String, prefixOne: String, prisonNumberTwo: String, prefixTwo: String) = """
  [{
    "prisonerNumber": "$prisonNumberOne",
    "pncNumber": "${randomPnc()}",
    "pncNumberCanonicalShort": "",
    "pncNumberCanonicalLong": "${randomPnc()}",
    "croNumber": "${randomCro()}",
    "bookingId": "0001200924",
    "bookNumber": "38412A",
    "firstName": "${prefixOne}FirstName",
    "middleNames": "${prefixOne}MiddleNameOne ${prefixOne}MiddleNameTwo",
    "lastName": "${prefixOne}LastName",
    "dateOfBirth": "${randomDate()}",
    "gender": "Female",
    "ethnicity": "White: Eng./Welsh/Scot./N.Irish/British",
    "youthOffender": true,
    "maritalStatus": "Widowed",
    "religion": "Church of England (Anglican)",
    "nationality": "Egyptian",
    "status": "ACTIVE IN",
    "lastMovementTypeCode": "CRT",
    "lastMovementReasonCode": "CA",
    "inOutStatus": "IN",
    "prisonId": "MDI",
    "lastPrisonId": "MDI",
    "prisonName": "HMP Leeds",
    "cellLocation": "A-1-002",
    "aliases": [
      {
        "firstName": "${prefixOne}AliasOneFirstName",
        "middleNames": "${prefixOne}AliasOneMiddleNameOne ${prefixOne}AliasOneMiddleNameTwo",
        "lastName": "${prefixOne}AliasOneLastName",
        "dateOfBirth": "${randomDate()}",
        "gender": "Male",
        "ethnicity": "White : Irish"
      },
  {
        "firstName": "${prefixOne}AliasTwoFirstName",
        "middleNames": "${prefixOne}AliasTwoMiddleNameOne ${prefixOne}AliasTwoMiddleNameTwo",
        "lastName": "${prefixOne}AliasTwoLastName",
        "dateOfBirth": "${randomDate()}",
        "gender": "Male",
        "ethnicity": "White : Irish"
      }
    ],
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
    ]
  },{
    "prisonerNumber": "$prisonNumberTwo",
    "pncNumber": "${randomPnc()}",
    "pncNumberCanonicalShort": "",
    "pncNumberCanonicalLong": "${randomPnc()}",
    "croNumber": "${randomCro()}",
    "bookingId": "0001200924",
    "bookNumber": "38412A",
    "firstName": "${prefixTwo}FirstName",
    "middleNames": "John James",
    "lastName": "${randomName()}",
    "dateOfBirth": "1975-04-02",
    "gender": "Female",
    "ethnicity": "White: Eng./Welsh/Scot./N.Irish/British",
    "youthOffender": true,
    "maritalStatus": "Widowed",
    "religion": "Church of England (Anglican)",
    "nationality": "Egyptian",
    "status": "ACTIVE IN",
    "lastMovementTypeCode": "CRT",
    "lastMovementReasonCode": "CA",
    "inOutStatus": "IN",
    "prisonId": "MDI",
    "lastPrisonId": "MDI",
    "prisonName": "HMP Leeds",
    "cellLocation": "A-1-002",
    "aliases": [],
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
    ]
  }]
  
""".trimIndent()
