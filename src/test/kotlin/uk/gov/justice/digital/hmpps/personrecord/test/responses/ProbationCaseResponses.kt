package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

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
          "forename": "${probationCase.firstName}",
          "middleName": "${probationCase.middleName}",
          "surname": "${probationCase.lastName}"
      },
      "title": {
        "code": "${probationCase.title ?: ""}",
        "description": "Example"
      },
      "dateOfBirth": "${probationCase.dateOfBirth ?: ""}",
      "gender": {
          "code": "${probationCase.gender ?: ""}",
          "description": "Male"
      },
      "aliases": [${probationCase.aliases.joinToString { alias(it) }}],
      "addresses": [${probationCase.addresses.joinToString { address(it) }}],
      "ethnicity": {
        ${probationCase.ethnicity?.let { """ "code": "${probationCase.ethnicity}" """.trimIndent() } ?: ""}
      },
      "nationality": {
        ${probationCase.nationality?.let { """ "code": "${probationCase.nationality}", """.trimIndent() } ?: ""}
        "description": "string"
      },
      "secondaryNationality": {
        ${probationCase.secondaryNationality?.let { """ "code": "${probationCase.secondaryNationality}", """.trimIndent() } ?: ""}
        "description": "string"
      },
      "contactDetails": {
        "telephone": "${probationCase.contacts.find { it.type == ContactType.HOME }?.value ?: ""}",
        "mobile": "${probationCase.contacts.find { it.type == ContactType.MOBILE }?.value ?: ""}",
        "email": "${probationCase.contacts.find { it.type == ContactType.EMAIL }?.value ?: ""}"
      },
      "sexualOrientation": {
        ${probationCase.sexualOrientation?.let { """ "code": "${probationCase.sexualOrientation}", """.trimIndent() } ?: ""}
        "description": "string"
      },
       "religion": {
      ${probationCase.religion?.let { """ "code": "${probationCase.religion}", """.trimIndent() } ?: ""}
      "description": "string"
    },
      "sentences": [${probationCase.sentences?.joinToString { sentence(it) } ?: ""}]
    }
""".trimIndent()

private fun alias(alias: ApiResponseSetupAlias) =
  """
    {
          "name": {
            "forename": "${alias.firstName ?: ""}",
            "middleName": "${alias.middleName ?: ""}",
            "surname": "${alias.lastName ?: ""}",
            "previousSurname": "previousSurname",
            "preferred": "preferred"
          },
          "dateOfBirth": "${alias.dateOfBirth ?: ""}",
          "gender": {
            ${alias.gender?.let { """ "code": "${alias.gender}", """.trimIndent() } ?: ""}
            "description": "string"
          }
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
