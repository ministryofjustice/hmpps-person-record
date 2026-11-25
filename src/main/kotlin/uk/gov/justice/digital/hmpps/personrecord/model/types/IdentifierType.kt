package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference

enum class IdentifierType(val description: String) {
  AAMR("Alcohol Abstinence Monitoring Requirement"),
  ACC("Acquisitive Crime"),
  APNC("Additional PNC"),
  AMRL("Alcohol Monitoring (SCRAMnet ID)"),
  ASN("Arrest Summons Number"),
  CID("Case Information Database (Immigration)"),
  URN("CPS Unique Reference Number"),
  CRO("Criminal Records Office Number"),
  TBRI("Did Not Enter Prison Tagged Bail Rel"),
  DRIVER_LICENSE_NUMBER("Driving Licence"), // DL in the spreadsheet - 75170 records in production
  DNOMS("Duplicate NOMIS Number"),
  XIMMN("Former Immigration Number"),
  XNOMS("Former NOMS Number"),
  HOREF("Home Office Reference No"),
  IMMN("Immigration Number"),
  DOFF("Known Duplicate CRN"),
  LCRN("Legacy CRN at Migration"),
  LBCN("Libra Breach Case Number"),
  LIFN("Lifer Number"),
  MFCRN("Merged From CRN"),
  MERGE_HMPS("Merged FROM Legacy LIDS Number"),
  MERGED("Merged FROM NOMS Number"),
  MTCRN("Merged To Case Reference Number"),
  MSVN("Military Service Number"),
  NATIONAL_INSURANCE_NUMBER("National Insurance Number"), // NINO in the spreadsheet - 248603 records in production
  NHS("NHS Number"),
  OTHR("Other Personal Identifier"),
  PCRN("Other Previous CRN"),
  PARK("Park Run Number"),
  PARN("Parole Number"),
  PST("Passport Number"), // PASS in the spreadsheet - 129 records in production
  PNC("PNC Number"),
  PORT_REF("Port Reference (Immigration)"),
  AI02("Previous NOMS Number"), // NOMS in the spreadsheet - 26041 records in production
  PRNOMS("Previously recorded as NOMS Number"),
  HMPS("Prison Legacy System Number"),
  NPD("Probation Legacy System Number"),
  SPNC("Scottish PNC Number"),
  SHARED_ALIAS("SHARED ALIAS WARNING"),
  STAFF("Staff Pass/ Identity Card"),
  SYSIDENT("System Identifier"),
  ULN("Unique Learner Number"),
  NPNC("Verified No PNC Date"),
  VISO("Violent and Sex Offender Register Number"),
  YJAF("Young Justice Application Framework Identifier"),
  YCRN("Youth Offending Team Identifier/CRN"),

  // existing CPR types have duplicate mappings - require migration
  DRL("Driving Licence"), // DELIUS USE - 1621 records in production - maps to our existing DRIVER_LICENSE_NUMBER and DL in the spreadsheet
  NOMS("Previous NOMS Number"), // NOMIS USE - 19 records in production - maps to our existing AI02 and NOMS in the spreadsheet

  // new types that replace existing ones - require migration
  DL("Driving Licence"), // replaces existing DRIVER_LICENSE_NUMBER and DRL
  NINO("National Insurance Number"), // replaces existing NATIONAL_INSURANCE_NUMBER
  PASS("Passport Number"), // replaces existing PST

  // internal use
  UNKNOWN("Unknown"),
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
