package uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.statementmodels

import java.time.LocalDate

data class PreparedDateStatement(
  val parameterName: String,
  val value : LocalDate?,
) {

  val day: PreparedIntStatement = PreparedIntStatement(
    parameterName = parameterName + DAY_PARAM_SUFFIX,
    value = value?.dayOfMonth
  )
  val month: PreparedIntStatement = PreparedIntStatement(
    parameterName = parameterName + MONTH_PARAM_SUFFIX,
    value = value?.monthValue
  )
  val year: PreparedIntStatement = PreparedIntStatement(
    parameterName = parameterName + YEAR_PARAM_SUFFIX,
    value = value?.year
  )

  companion object {
    private const val DAY_PARAM_SUFFIX = "Day"
    private const val MONTH_PARAM_SUFFIX = "Month"
    private const val YEAR_PARAM_SUFFIX = "Year"
  }
}
