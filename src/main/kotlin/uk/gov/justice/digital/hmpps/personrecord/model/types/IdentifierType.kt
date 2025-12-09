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
  NINO,
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
  ;

  companion object {
    val probationAdditionalIdentifiers = listOf(
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
      NINO,
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

    private fun createBaseIdentifierReferences(probationCase: ProbationCase): List<Reference> = listOf(
      Reference(identifierType = CRO, identifierValue = CROIdentifier.from(probationCase.identifiers.cro).croId),
      Reference(identifierType = PNC, identifierValue = PNCIdentifier.from(probationCase.identifiers.pnc).pncId),
      Reference(
        identifierType = NATIONAL_INSURANCE_NUMBER,
        identifierValue = probationCase.identifiers.nationalInsuranceNumber,
      ),
    )

    fun createProbationIdentifierReferences(probationCase: ProbationCase): List<Reference> {
      val identifiers = createBaseIdentifierReferences(probationCase)

      val additionalIdentifiers: List<Reference> = probationCase.identifiers.additionalIdentifiers?.map {
        Reference(
          identifierType = IdentifierType.valueOf(it.type?.value!!),
          identifierValue = it.value,
        )
      } ?: emptyList()

      return identifiers + additionalIdentifiers
    }
  }
}
