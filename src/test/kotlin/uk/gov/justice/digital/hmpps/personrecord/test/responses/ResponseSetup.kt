package uk.gov.justice.digital.hmpps.personrecord.test.responses

data class ApiResponseSetupAddress(val postcode: String)

data class ApiResponseSetup(val crn: String, val cro: String, val pnc: String? = null, val prefix: String, val prisonNumber: String, val addresses: List<ApiResponseSetupAddress>, val nationalInsuranceNumber: String)
