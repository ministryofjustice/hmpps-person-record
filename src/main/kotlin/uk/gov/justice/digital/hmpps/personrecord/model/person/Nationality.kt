package uk.gov.justice.digital.hmpps.personrecord.model.person
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import java.time.LocalDate

data class Nationality(
  var code: NationalityCode,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val notes: String? = null,
)
