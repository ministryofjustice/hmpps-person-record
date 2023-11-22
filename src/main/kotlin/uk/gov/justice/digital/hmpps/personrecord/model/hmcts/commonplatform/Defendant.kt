package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
class Defendant(
  private val pncId: String? = null,
  private val croNumber: String? = null,
  @Valid
  private val personDefendant: PersonDefendant? = null,
)
