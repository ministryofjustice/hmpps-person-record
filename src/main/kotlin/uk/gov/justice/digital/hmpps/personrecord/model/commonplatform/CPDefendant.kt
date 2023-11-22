package uk.gov.justice.digital.hmpps.personrecord.model.commonplatform

import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.Valid

@JsonInclude(JsonInclude.Include.NON_NULL)
class CPDefendant(
  private val pncId: String? = null,
  private val croNumber: String? = null,
  @Valid
  private val personDefendant: CPPersonDefendant? = null,
)
