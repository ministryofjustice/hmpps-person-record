package uk.gov.justice.digital.hmpps.personrecord.model.types

interface ReferenceData {
  val description: String
  val current: Boolean
  val applicableTo: Set<ReferenceDataDomain>
}
// get() = setOf(ReferenceDataDomain.PROBATION, ReferenceDataDomain.PRISON)
