package uk.gov.justice.digital.hmpps.personrecord.client.model

data class Name(
    val forename: String? = null,
    val surname: String? = null,
    val otherNames: List<String>? = emptyList()
)
