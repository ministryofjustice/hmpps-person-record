package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAdditionalIdentifier
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank

enum class AdditionalIdentifierCode(val description: String) {
  AAMR("AAMR"),
  ACC("Acquisitive Crime"),
  APNC("Additional PNC"),
  AMRL("Alcohol Monitoring (SCRAMnet ID)"),
  ASN("Arrest Summons Number"),
  URN("CPS Unique Reference Number"),
  DRL("Drivers Licence"),
  DNOMS("Duplicate NOMIS Number"),
  XIMMN("Former Immigration Number"),
  XNOMS("Former NOMS Number"),
  IMMN("Immigration Number"),
  DOFF("Known Duplicate CRN"),
  LCRN("Legacy CRN at Migration"),
  LBCN("Libra Breach Case Number"),
  LIFN("Lifer Number"),
  MFCRN("Merged From CRN"),
  MTCRN("Merged To CRN"),
  MSVN("Military Service Number"),
  NINO("National Insurance Number"),
  NHS("NHS Number"),
  NOMS("NOMIS Number"),
  OTHR("Other Personal Identifier"),
  PCRN("Other Previous CRN"),
  PARN("Parole Number"),
  PST("Passport Number"),
  AI02("Previous Prison Number"),
  PRNOMS("Previously recorded as NOMS Number"),
  SPNC("Scottish/Old PNC Number"),
  NPNC("Verified No PNC Date"),
  VISO("ViSOR Number"),
  YCRN("YOT Identifier/CRN"),
  ;

  companion object {

    val probationAdditionalIdentifierCode: Map<String, AdditionalIdentifierCode> = entries.associateBy { it.name }

    fun from(probationCaseAdditionalIdentifier: ProbationCaseAdditionalIdentifier?): AdditionalIdentifierCode? = probationCaseAdditionalIdentifier?.type?.value.nullIfBlank()?.let {
      probationAdditionalIdentifierCode.getOrDefault(it, null)
    }
  }
}
