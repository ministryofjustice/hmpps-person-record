package uk.gov.justice.digital.hmpps.personrecord.client.model.offender

import java.time.LocalDate

data class ProbationAddress(
  val noFixedAbode: Boolean? = null,
  val startDate: LocalDate? = null,
  val endDate: LocalDate? = null,
  val postcode: String? = null,
  val fullAddress: String? = null,
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val district: String? = null,
  val townCity: String? = null,
  val county: String? = null,
  val uprn: String? = null,
  val notes: String? = null,
  val telephoneNumber: String? = null,
  val addressId: String? = null,
)

/**
 *   "id": 0,
 *   "fullAddress": "string",
 *   "buildingName": "string",
 *   "addressNumber": "string",
 *   "streetName": "string",
 *   "district": "string",
 *   "townCity": "string",
 *   "county": "string",
 *   "postcode": "string",
 *   "uprn": 0,
 *   "telephoneNumber": "string",
 *   "noFixedAbode": true,
 *   "status": {
 *     "code": "string",
 *     "description": "string"
 *   },
 *   "notes": "string",
 *   "startDate": "2026-04-30",
 *   "endDate": "2026-04-30"
 */
