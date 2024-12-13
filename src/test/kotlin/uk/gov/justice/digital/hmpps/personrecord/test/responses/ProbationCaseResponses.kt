package uk.gov.justice.digital.hmpps.personrecord.test.responses

fun probationCaseResponse(probationCase: ApiResponseSetup) = """
    {
      "identifiers": {
          "deliusId": 2500000501,
          ${probationCase.pnc?.let { """ "pnc": "${probationCase.pnc}", """.trimIndent() } ?: ""}
          "crn": "${probationCase.crn ?: ""}",
          "cro": "${probationCase.cro ?: ""}",
          "prisonerNumber": "${probationCase.prisonNumber ?: ""}",
          "ni": "${probationCase.nationalInsuranceNumber ?: ""}"
      },
      "name": {
          "forename": "${probationCase.prefix ?: ""}${probationCase.firstName ?: "" }",
          "middleName": "PreferredMiddleName",
          "surname": "${probationCase.prefix ?: ""}${probationCase.lastName ?: ""}"
      },
      "title": {
        "code": "${probationCase.title ?: ""}",
        "description": "Example"
      },
      "dateOfBirth": "${probationCase.dateOfBirth ?: ""}",
      "gender": {
          "code": "M",
          "description": "Male"
      },
      "aliases": [
        {
          "name": {
            "forename": "${probationCase.prefix ?: ""}FirstName",
            "middleName": "MiddleName",
            "surname": "${probationCase.prefix ?: ""}LastName",
            "previousSurname": "string",
            "preferred": "string"
          },
          "dateOfBirth": "2024-05-30"
        }
      ],
      "addresses": [${probationCase.addresses.joinToString { address(it) }}],
      "ethnicity": {
        ${probationCase.ethnicity?.let { """ "code": "${probationCase.ethnicity}" """.trimIndent() } ?: ""}
      },
      "nationality": {
        ${probationCase.nationality?.let { """ "code": "${probationCase.nationality}", """.trimIndent() } ?: ""}
        "description": "string"
      },
      "contactDetails": {
        "telephone": "01234567890",
        "mobile": "01234567890",
        "email": "test@gmail.com"
      },
      "sentences": [${probationCase.sentences?.joinToString { sentence(it) } ?: ""}]
    }
""".trimIndent()

private fun address(address: ApiResponseSetupAddress) =
  """
    {
      ${address.startDate?.let { """ "startDate": "${address.startDate}", """.trimIndent() } ?: ""}
      ${address.endDate?.let { """ "endDate": "${address.endDate}", """.trimIndent() } ?: ""}
      ${address.noFixedAbode?.let { """ "noFixedAbode": "${address.noFixedAbode}", """.trimIndent() } ?: ""}
      ${address.fullAddress?.let { """ "fullAddress": "${address.fullAddress}", """.trimIndent() } ?: ""}
      ${address.postcode?.let { """ "postcode": "${address.postcode}" """.trimIndent() } ?: ""}
    }
  """.trimIndent()

private fun sentence(sentence: ApiResponseSetupSentences) =
  """
    {
      ${sentence.sentenceDate?.let { """ "date": "${sentence.sentenceDate}" """ }}
    }
  """.trimIndent()
