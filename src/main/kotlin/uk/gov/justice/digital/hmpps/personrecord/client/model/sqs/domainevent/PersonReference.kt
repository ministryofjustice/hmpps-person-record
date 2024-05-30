package uk.gov.justice.digital.hmpps.personrecord.client.model.sqs.domainevent

import com.fasterxml.jackson.annotation.JsonProperty

data class PersonReference(
  val identifiers: List<PersonIdentifier>? = emptyList(),
)
