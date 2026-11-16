package uk.gov.justice.digital.hmpps.personrecord.model.types

import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.CPR
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PRISON
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReferenceDataDomain.PROBATION

enum class AddressUsageCode(
  override val description: String,
  override val current: Boolean,
  override val applicableTo: Set<ReferenceDataDomain>,
) : ReferenceData {
  // PRISON
  CARE("Care or Old Persons Home", true, setOf(PRISON)),
  CURFEW("Curfew Order", true, setOf(PRISON)),
  DAP("Discharge - Approved Premises", true, setOf(PRISON)),
  DBA("Discharge - BASS Accommodation", true, setOf(PRISON)),
  DBH("Discharge - Bail/Probation Hostel", true, setOf(PRISON)),
  DNF("Discharge - NFA", true, setOf(PRISON)),
  DOH("Discharge - Other Hostel", true, setOf(PRISON)),
  DPH("Discharge - Permanent Housing", true, setOf(PRISON)),
  DSH("Discharge - Supported Housing", true, setOf(PRISON)),
  DST("Discharge - Transient/Short Term", true, setOf(PRISON)),
  DUT("Discharge - Unknown Accommodation Type", true, setOf(PRISON)),
  HDC("HDC Address", true, setOf(PRISON)),
  HDC2("2nd HDC Address", true, setOf(PRISON)),
  HOME("Home", true, setOf(PRISON)),
  HOSP("Hospital", true, setOf(PRISON)),
  HOST("Approved Premises", true, setOf(PRISON)),
  OTHER("Other", true, setOf(PRISON)),
  RECEP("Reception", true, setOf(PRISON)),
  RELEASE("Discharge", true, setOf(PRISON)),
  RES("Residency Condition", true, setOf(PRISON)),
  ROTL("Release on Temporary Licence", true, setOf(PRISON)),

  // PROBATION
  A02("Approved Premises", true, setOf(PROBATION)),
  A16("Awaiting Assessment", true, setOf(PROBATION)),
  A10("CAS2/BASS accommodation 13 weeks or more", true, setOf(PROBATION)),
  A11("CAS2/BASS accommodation less than 13 weeks", true, setOf(PROBATION)),
  A17("CAS3", true, setOf(PROBATION)),
  A07B("Friends/Family (settled)", true, setOf(PROBATION)),
  A07A("Friends/Family (transient)", true, setOf(PROBATION)),
  A14("HOIE Section 10", true, setOf(PROBATION)),
  A13("HOIE Section 4", true, setOf(PROBATION)),
  A08A("Homeless - Rough Sleeping", true, setOf(PROBATION)),
  A08C("Homeless - Shelter/Emergency Hostel/Campsite", true, setOf(PROBATION)),
  A08("Homeless - Squat", true, setOf(PROBATION)),
  A01A("Householder (Owner - freehold or leasehold)", true, setOf(PROBATION)),
  A15("Immigration Detention", true, setOf(PROBATION)),
  A12("Long Term Residential Healthcare", true, setOf(PROBATION)),
  A01C("Rental accommodation - private rental", true, setOf(PROBATION)),
  A01D("Rental accommodation - social rental (LA or other)", true, setOf(PROBATION)),
  A04("Supported Housing", true, setOf(PROBATION)),
  A03("Transient/short term accommodation", true, setOf(PROBATION)),
  A01("Permanent Independent Housing", false, setOf(PROBATION)),
  A01B("Permanent Independent Housing (LA or private rent)", false, setOf(PROBATION)),
  A05("No fixed abode", false, setOf(PROBATION)),
  A06("No information", false, setOf(PROBATION)),
  A07("Friends/Family", false, setOf(PROBATION)),
  A08B("Homeless - Other", false, setOf(PROBATION)),
  A96("Multiple Personal Circumstances Identified", false, setOf(PROBATION)),
  A97("Unsuitable Accommodation (Migrated Record)", false, setOf(PROBATION)),
  A98("Suitable Accommodation (Migrated Record)", false, setOf(PROBATION)),
  A99("Historic Accommodation Record", false, setOf(PROBATION)),
  CS02("Multi-occupancy housing", false, setOf(PROBATION)),
  CS03("Partner's home", false, setOf(PROBATION)),
  CS04("Hospital / Institution", false, setOf(PROBATION)),
  CS05("Custody", false, setOf(PROBATION)),
  CS06("Case File Checked Out", false, setOf(PROBATION)),

  // CPR
  UNKNOWN("Unknown", false, setOf(CPR)),
  ;

  companion object {
    fun from(value: String): AddressUsageCode = entries.associateBy { it.name }.getOrDefault(value, UNKNOWN)
  }
}
