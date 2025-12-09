package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
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

    fun createAdditionalIdentifierReferences(probationCase: ProbationCase): List<Reference> {
      val incomingTypes = probationCase.identifiers.additionalIdentifiers?.map { it.type?.value }
      val incomingValues = probationCase.identifiers.additionalIdentifiers?.map { it.value }

      val additionalIdentifiers = IdentifierType.probationAdditionalIdentifiers.values
        .filter { incomingTypes?.contains(it.name) == true }
        .mapIndexed { index, element -> Reference(identifierType = element, identifierValue = incomingValues?.get(index)) }

      return additionalIdentifiers
    }

  }
}
