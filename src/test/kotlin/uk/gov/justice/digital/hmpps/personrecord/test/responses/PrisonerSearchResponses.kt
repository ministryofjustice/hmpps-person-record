package uk.gov.justice.digital.hmpps.personrecord.test.responses

fun prisonerSearchResponse(
  prisonNumber: String,
  pnc: String?,
  cro: String?,
  firstName: String?,
  lastName: String?,
) = """
  {
    "prisonerNumber": "$prisonNumber",
    "pncNumber": "$pnc",
    "pncNumberCanonicalShort": "${pnc?.takeLast(11)}",
    "pncNumberCanonicalLong": "$pnc",
    "croNumber": "$cro",
    "bookingId": "0001200924",
    "bookNumber": "38412A",
    "title": "Ms",
    "firstName": "$firstName",
    "middleNames": "John James",
    "lastName": "$lastName",
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
        "title": "Ms",
        "firstName": "Robert",
        "middleNames": "Trevor",
        "lastName": "Lorsen",
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
    ],
    "addresses": [
      {
        "fullAddress": "1 Main Street, Crookes, Sheffield, South Yorkshire, S10 1BP, England",
        "postalCode": "S10 1BP",
        "startDate": "2020-07-17",
        "primaryAddress": true,
        "phoneNumbers": [
          {
            "type": "HOME, MOB",
            "number": "01141234567"
          }
        ]
      }
    ],
    "emailAddresses": [
      {
        "email": "john.smith@gmail.com"
      }
    ],
    "phoneNumbers": [
      {
        "type": "HOME, MOB",
        "number": "01141234567"
      }
    ],
    "identifiers": [
      {
        "type": "PNC, CRO, DL, NINO",
        "value": "12/394773H",
        "issuedDate": "2020-07-17",
        "issuedAuthorityText": "string",
        "createdDateTime": "2021-07-05T10:35:17"
      }
    ],
    "allConvictedOffences": [
      {
        "statuteCode": "TH68",
        "offenceCode": "TH68010",
        "offenceDescription": "Theft from a shop",
        "offenceDate": "2024-05-23",
        "latestBooking": true
      }
    ]
  }
""".trimIndent()
