package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent

data class DomainEvent(
  val eventType: String,
  val detailUrl: String,
  val personReference: PersonReference? = null,
  val additionalInformation: AdditionalInformation?,
)
