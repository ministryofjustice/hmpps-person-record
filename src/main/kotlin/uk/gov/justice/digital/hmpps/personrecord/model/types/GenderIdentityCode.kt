package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.CPR
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class GenderIdentityCode(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {
  GIF("Female", true, setOf(PROBATION)),
  GIM("Male", true, setOf(PROBATION)),
  GINB("Non-Binary", true, setOf(PROBATION)),
  GIRF("Prefer not to say", true, setOf(PROBATION)),
  GISD("Prefer to self-describe", true, setOf(PROBATION)),
  UNK("Unknown", true, setOf(CPR)),
  ;

  companion object {

    val probationGenderIdentityCode: Map<String, GenderIdentityCode> = GenderIdentityCode.entries.associateBy { it.name }

    fun from(probationCase: ProbationCase?): GenderIdentityCode? = probationCase?.genderIdentity?.value.nullIfBlank()?.let {
      probationGenderIdentityCode.getOrDefault(it, UNK)
    }
  }
}
