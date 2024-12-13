package uk.gov.justice.digital.hmpps.personrecord.test.responses

fun allProbationCasesSingleResponse(firstCrn: String, firstPrefix: String) = """
  {
    "content": [
        {
            "identifiers": {
                "deliusId": 2500000501,
                "crn": "$firstCrn"
            },
            "name": {
                "forename": "${firstPrefix}FirstName",
                "surname": "${firstPrefix}LastName"
            },
            "dateOfBirth": "1980-08-29",
            "gender": {
                "code": "M",
                "description": "Male"
            },
            "aliases": [],
            "addresses": []
        }
    ],
    "page": {
        "size": 1,
        "number": 1,
        "totalElements": 1,
        "totalPages": 1
    }
 }
""".trimIndent()

fun allProbationCasesResponse(firstCrn: String, firstPrefix: String, secondCrn: String, secondPrefix: String, totalPages: Int = 4) = """
  {
    "content": [
        {
            "identifiers": {
                "deliusId": 2500000501,
                "crn": "$firstCrn"
            },
            "name": {
                "forename": "${firstPrefix}FirstName",
           "middleName":"${firstPrefix}MiddleNameOne ${firstPrefix}MiddleNameTwo",     "surname": "${firstPrefix}LastName"
            },
            "dateOfBirth": "1980-08-29",
            "gender": {
                "code": "M",
                "description": "Male"
            },
            "aliases": [{"name": {
                        "forename": "${firstPrefix}AliasOneFirstName",
                     "middleName":    "${firstPrefix}AliasOneMiddleNameOne ${firstPrefix}AliasOneMiddleNameTwo",
                        "surname": "${firstPrefix}AliasOneLastName"
                    },
                    "dateOfBirth": "1967-11-04"},{"name": {
                        "forename": "${firstPrefix}AliasTwoFirstName",
            "middleName":             "${firstPrefix}AliasTwoMiddleNameOne ${firstPrefix}AliasTwoMiddleNameTwo",
                        "surname": "${firstPrefix}AliasTwoLastName"
                    },
                    "dateOfBirth": "1967-11-04"}],
            "addresses": []
        },
        {
            "identifiers": {
                "deliusId": 2500000503,
                "crn": "$secondCrn"
            },
            "name": {
                "forename": "${secondPrefix}FirstName",
           "middleName":"${secondPrefix}MiddleNameOne ${secondPrefix}MiddleNameTwo",     "surname": "${secondPrefix}LastName"
            },
            "dateOfBirth": "1990-05-18",
            "gender": {
                "code": "M",
                "description": "Male"
            },
            "aliases": [],
            "addresses": []
        }
    ],
    "page": {
        "size": 2,
        "number": 10,
        "totalElements": 102,
        "totalPages": $totalPages
    }
 }
""".trimIndent()
