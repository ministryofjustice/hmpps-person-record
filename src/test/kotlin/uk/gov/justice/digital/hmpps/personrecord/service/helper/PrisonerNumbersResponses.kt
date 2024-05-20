package uk.gov.justice.digital.hmpps.personrecord.service.helper

fun prisonerNumbersResponse(prisonerNumbers: List<String>) = """
{
  "totalPages": 4,
  "totalElements": 7,
  "first": true,
  "last": false,
  "size": 0,
  "content": [
    "${prisonerNumbers.joinToString("\",\"")}"
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
