package uk.gov.justice.digital.hmpps.personrecord.test.responses

import uk.gov.justice.digital.hmpps.personrecord.message.listeners.ProbationCaseResponseSetup

fun probationCaseResponse(probationCase: ProbationCaseResponseSetup) = """
        {
            "identifiers": {
                "deliusId": 2500000501,
                "crn": "${probationCase.crn}"
                ${probationCase.pnc?.let { """ ,"pnc": "${probationCase.pnc}" """.trimIndent() } ?: ""}
            },
            "name": {
                "forename": "${probationCase.prefix}FirstName",
                "surname": "${probationCase.prefix}LastName"
            },
            "dateOfBirth": "1980-08-29",
            "gender": {
                "code": "M",
                "description": "Male"
            },
            "aliases": [],
            "addresses": []
        }]
""".trimIndent()
