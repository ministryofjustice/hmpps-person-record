package uk.gov.justice.digital.hmpps.personrecord.model

import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.AdditionalInformation

data class DomainEvent(
  val eventType: String,
  val detailUrl: String,
  val personReference: PersonReference? = null,
  val additionalInformation: AdditionalInformation?,
)
