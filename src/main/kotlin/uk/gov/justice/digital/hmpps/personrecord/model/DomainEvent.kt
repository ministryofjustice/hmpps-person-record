package uk.gov.justice.digital.hmpps.personrecord.model


data class DomainEvent(
  val eventType: String,
  val detailUrl: String,
  val personReference: PersonReference? = null
)
