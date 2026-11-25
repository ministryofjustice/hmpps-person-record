package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference

enum class IdentifierType(val description: String, val current: Boolean) {
  AAMR("Alcohol Abstinence Monitoring Requirement", false),
  ACC("Acquisitive Crime", true),
  APNC("Additional PNC", true),
  AMRL("Alcohol Monitoring (SCRAMnet ID)", true),
  ARREST_SUMMONS_NUMBER("Arrest Summons Number", true), // TODO: ASN in the spreadsheet - there are 0 records in production
  CID("Case Information Database (Immigration)", true),
  URN("CPS Unique Reference Number", true),
  CRO("Criminal Records Office Number", true),
  TBRI("Did Not Enter Prison Tagged Bail Rel", true),
  DRIVER_LICENSE_NUMBER("Driving Licence", true), // TODO: DL in the spreadsheet - 75170 records in production
  DNOMS("Duplicate NOMIS Number", true),
  XIMMN("Former Immigration Number", false),
  XNOMS("Former NOMS Number", false),
  HOREF("Home Office Reference No", true),
  IMMN("Immigration Number", true),
  DOFF("Known Duplicate CRN", true),
  LCRN("Legacy CRN at Migration", false),
  LBCN("Libra Breach Case Number", true),
  LIFN("Lifer Number", true),
  MFCRN("Merged From CRN", false),
  MERGE_HMPS("Merged FROM Legacy LIDS Number", false),
  MERGED("Merged FROM NOMS Number", false),
  MTCRN("Merged To Case Reference Number", false),
  MSVN("Military Service Number", false),
  NATIONAL_INSURANCE_NUMBER("National Insurance Number", true), // TODO: NINO in the spreadsheet - 248603 records in production
  NHS("NHS Number", true),
  BOOK("NOMIS Book Number", true), // TODO: red in the spreadsheet - do we need this new one???
  OTHR("Other Personal Identifier", true),
  PCRN("Other Previous CRN", true),
  PARK("Park Run Number", true),
  PARN("Parole Number", false),
  PST("Passport Number", true), // TODO: PASS in the spreadsheet - 129 records in production
  PNC("PNC Number", true),
  PORT_REF("Port Reference (Immigration)", true),
  AI02("Previous NOMS Number", false), // TODO: NOMS in the spreadsheet - 26041 records in production
  PRNOMS("Previously recorded as NOMS Number", false),
  HMPS("Prison Legacy System Number", true),
  NPD("Probation Legacy System Number", true),
  SPNC("Scottish PNC Number", true),
  SHARED_ALIAS("SHARED ALIAS WARNING", true),
  STAFF("Staff Pass/ Identity Card", true),
  SYSIDENT("System Identifier", false),
  ULN("Unique Learner Number", true),
  NPNC("Verified No PNC Date", true),
  VISO("Violent and Sex Offender Register Number", true),
  YJAF("Young Justice Application Framework Identifier", true),
  YCRN("Youth Offending Team Identifier/CRN", true),

  // existing CPR types have duplicate mappings - require migration
  ASN("Arrest Summons Number", true), // TODO: DELIUS USE - 22858 records in production - maps to our existing ARREST_SUMMONS_NUMBER and ASN in the spreadsheet
  DRL("Driving Licence", true), // TODO: DELIUS USE - 1621 records in production - maps to our existing DRIVER_LICENSE_NUMBER and DL in the spreadsheet
  NOMS("Previous NOMS Number", false), // TODO: NOMIS USE - 19 records in production - maps to our existing AI02 and NOMS in the spreadsheet

  // new types that replace existing ones - require migration
  DL("Driving Licence", true), // TODO: replaces existing DRIVER_LICENSE_NUMBER and DRL
  NINO("National Insurance Number", true), // TODO: replaces existing NATIONAL_INSURANCE_NUMBER
  PASS("Passport Number", true), // TODO: replaces existing PST

  // internal use
  UNKNOWN("Unknown", false),
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
