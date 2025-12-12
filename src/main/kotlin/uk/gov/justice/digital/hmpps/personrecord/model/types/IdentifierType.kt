package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference

enum class IdentifierType {
  PNC,
  CRO,
  NATIONAL_INSURANCE_NUMBER,
  DRIVER_LICENSE_NUMBER,
  ARREST_SUMMONS_NUMBER,
  AAMR,
  ACC,
  APNC,
  AMRL,
  ASN,
  URN,
  DRL,
  DNOMS,
  XIMMN,
  XNOMS,
  IMMN,
  DOFF,
  LCRN,
  LBCN,
  LIFN,
  MFCRN,
  MTCRN,
  MSVN,
  NHS,
  NOMS,
  OTHR,
  PCRN,
  PARN,
  PST,
  AI02,
  PRNOMS,
  SPNC,
  NPNC,
  VISO,
  YCRN,
  UNKNOWN,
  ;

  companion object {
    val probationAdditionalIdentifiersWithoutNino = listOf(
      AAMR,
      ACC,
      APNC,
      AMRL,
      ASN,
      URN,
      DRL,
      DNOMS,
      XIMMN,
      XNOMS,
      IMMN,
      DOFF,
      LCRN,
      LBCN,
      LIFN,
      MFCRN,
      MTCRN,
      MSVN,
      NHS,
      NOMS,
      OTHR,
      PCRN,
      PARN,
      PST,
      AI02,
      PRNOMS,
      SPNC,
      NPNC,
      VISO,
      YCRN,
    ).associateBy { it.name }

    val probationAdditionalIdentifiers: Map<String, IdentifierType> = mutableMapOf(
      "NINO" to NATIONAL_INSURANCE_NUMBER,
    ).plus(probationAdditionalIdentifiersWithoutNino)

    private fun createBaseIdentifierReferences(probationCase: ProbationCase): List<Reference> = listOf(
      Reference(CRO, CROIdentifier.from(probationCase.identifiers.cro).croId),
      Reference(PNC, PNCIdentifier.from(probationCase.identifiers.pnc).pncId),
      Reference(NATIONAL_INSURANCE_NUMBER, probationCase.identifiers.nationalInsuranceNumber),
    )

    fun createProbationIdentifierReferences(probationCase: ProbationCase): List<Reference> {
      val identifiers = createBaseIdentifierReferences(probationCase)

      val additionalIdentifiers: List<Reference> = removeDuplicateNationalInsuranceNumber(probationCase)?.map {
        Reference(
          probationAdditionalIdentifiers.getOrDefault(it.type?.value!!, UNKNOWN),
          it.value,
        )
      } ?: emptyList()

      return identifiers + additionalIdentifiers
    }

    private fun removeDuplicateNationalInsuranceNumber(probationCase: ProbationCase) = probationCase
      .identifiers
      .additionalIdentifiers?.filterNot {
        it.type?.value == "NINO" && it.value == probationCase.identifiers.nationalInsuranceNumber
      }
  }
}
