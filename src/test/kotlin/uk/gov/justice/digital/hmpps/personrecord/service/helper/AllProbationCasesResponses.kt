package uk.gov.justice.digital.hmpps.personrecord.service.helper

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
        }],
    "pageable": {
        "pageNumber": 6004,
        "pageSize": 100,
        "sort": {
            "unsorted": false,
            "sorted": true,
            "empty": false
        },
        "offset": 600400,
        "paged": true,
        "unpaged": false
    },
    "totalElements": 7,
    "totalPages": 4,
    "last": false,
    "numberOfElements": 2,
    "first": true,
    "size": 2,
    "number": 1,
    "sort": {
        "unsorted": false,
        "sorted": true,
        "empty": false
 },
    "empty": false}
""".trimIndent()
fun allProbationCasesResponse(firstCrn: String, firstPrefix: String, secondCrn: String, secondPrefix: String) = """
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
        }],
    "pageable": {
        "pageNumber": 6004,
        "pageSize": 100,
        "sort": {
            "unsorted": false,
            "sorted": true,
            "empty": false
        },
        "offset": 600400,
        "paged": true,
        "unpaged": false
    },
    "totalElements": 7,
    "totalPages": 4,
    "last": false,
    "numberOfElements": 2,
    "first": true,
    "size": 2,
    "number": 1,
    "sort": {
        "unsorted": false,
        "sorted": true,
        "empty": false
 },
    "empty": false}
""".trimIndent()
