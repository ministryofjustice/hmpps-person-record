package uk.gov.justice.digital.hmpps.personrecord.seeding.responses

fun prisonNumbersResponse(prisonNumbers: List<String?>, totalPages: Int = 4) = """
{
  "totalPages": $totalPages,
  "totalElements": 7,
  "first": true,
  "last": false,
  "size": 0,
  "content": [
    "${prisonNumbers.filterNotNull().joinToString("\",\"")}"
  ],
  "number": 0,
  "sort": [
    {
      "direction": "string",
      "nullHandling": "string",
      "ascending": true,
      "property": "string",
      "ignoreCase": true
    }
  ],
  "numberOfElements": 0,
  "pageable": {
    "offset": 0,
    "sort": [
      {
        "direction": "string",
        "nullHandling": "string",
        "ascending": true,
        "property": "string",
        "ignoreCase": true
      }
    ],
    "pageSize": 0,
    "pageNumber": 0,
    "unpaged": true,
    "paged": true
  },
  "empty": true
}
""".trimIndent()
