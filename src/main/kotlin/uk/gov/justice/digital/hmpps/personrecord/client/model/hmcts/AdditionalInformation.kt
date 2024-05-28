package uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts

data class AdditionalInformation(
  val categoriesChanged: List<String>? = emptyList(),
  val nomsNumber: String,
)
