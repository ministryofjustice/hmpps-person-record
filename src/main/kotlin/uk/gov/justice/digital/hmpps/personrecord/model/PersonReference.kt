package uk.gov.justice.digital.hmpps.personrecord.model

data class PersonReference(
    val identifiers: List<PersonIdentifier>? = emptyList()
)
