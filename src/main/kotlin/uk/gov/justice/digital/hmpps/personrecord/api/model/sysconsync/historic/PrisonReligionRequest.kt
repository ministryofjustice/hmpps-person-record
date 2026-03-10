package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.historic

import io.swagger.v3.oas.annotations.Hidden
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

data class PrisonReligionRequest(
  @Valid
  @NotEmpty
  val religions: List<PrisonReligion>,
) {

  @Hidden
  fun getCurrentReligion(): PrisonReligion? {
    val currentReligionCount = this.religions.filter { it.current }
    return when {
      currentReligionCount.size != 1 -> null
      else -> currentReligionCount.first()
    }
  }
}
