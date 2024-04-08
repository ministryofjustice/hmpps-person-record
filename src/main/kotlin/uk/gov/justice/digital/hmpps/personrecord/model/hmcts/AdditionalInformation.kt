package uk.gov.justice.digital.hmpps.personrecord.model.hmcts

data class AdditionalInformation(
  val categoriesChanged: List<String>? = emptyList(),
  val nomsNumber: String?,
)
