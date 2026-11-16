package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.CPR
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PRISON
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class AddressStatusCode(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {

  B("Bail", true, setOf(PROBATION)),
  M("Main", true, setOf(PROBATION, PRISON)),
  MA("Postal", true, setOf(PROBATION, PRISON)),
  P("Previous", true, setOf(PROBATION)),
  PR("Proposed", true, setOf(PROBATION)),
  PR1("Proposed for Resettlement", true, setOf(PROBATION)),
  RJ("Rejected", true, setOf(PROBATION)),
  RT("ROTL", true, setOf(PROBATION)),
  S("Secondary", true, setOf(PROBATION)),
  PM("Primary and Mail", true, setOf(CPR)),
  UNKNOWN("Unknown", true, setOf(CPR)),
  ;

  companion object {
    fun fromProbation(value: String): AddressStatusCode = entries.associateBy { it.name }.getOrDefault(value, UNKNOWN)

    fun fromPrison(isPrimary: Boolean, isMail: Boolean): AddressStatusCode? = when {
      isPrimary && isMail -> PM
      isPrimary -> M
      isMail -> MA
      else -> null
    }
  }
}
