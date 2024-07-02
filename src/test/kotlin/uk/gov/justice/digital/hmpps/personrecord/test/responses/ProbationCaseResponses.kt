package uk.gov.justice.digital.hmpps.personrecord.test.responses

fun probationCaseResponse(probationCase: ApiResponseSetup) = """
    {
      "identifiers": {
          "deliusId": 2500000501,
          ${probationCase.pnc?.let { """ "pnc": "${probationCase.pnc}", """.trimIndent() } ?: ""}
          "crn": "${probationCase.crn}",
          "cro": "075715/64Q",
          "prisonerNumber": "${probationCase.prisonNumber}",
          "ni": "1234567890"
      },
      "name": {
          "forename": "${probationCase.prefix}FirstName",
          "middleName": "PreferredMiddleName",
          "surname": "${probationCase.prefix}LastName"
      },
      "title": {
        "code": "Mr",
        "description": "Example"
      },
      "dateOfBirth": "1980-08-29",
      "gender": {
          "code": "M",
          "description": "Male"
      },
      "aliases": [
        {
          "name": {
            "forename": "${probationCase.prefix}FirstName",
            "middleName": "MiddleName",
            "surname": "${probationCase.prefix}LastName",
            "previousSurname": "string",
            "preferred": "string"
          },
          "dateOfBirth": "2024-05-30"
        }
      ],
      "addresses": [${probationCase.addresses.joinToString { address(it) }}],
      "contactDetails": {
        "telephone": "01234567890",
        "mobile": "01234567890",
        "email": "test@gmail.com"
      }
    }
""".trimIndent()

private fun address(address: ApiResponseSetupAddress) =
  """
                  {
                    "postcode": "${address.postcode}"
                  }
  """.trimIndent()
