package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class ContactType(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {
  HOME("Home", true, setOf(PROBATION)),
  BUS("Business", true, setOf(PROBATION)),
  FAX("Fax", true, setOf(PROBATION)),
  ALTB("Alternate Business", true, setOf(PROBATION)),
  ALTH("Alternate Home", true, setOf(PROBATION)),
  MOBILE("Mobile", true, setOf(PROBATION)),
  VISIT("Agency Visit Line", true, setOf(PROBATION)),
  EMAIL("Email", true, setOf(PROBATION)),
}
