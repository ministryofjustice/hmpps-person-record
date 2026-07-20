package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class AddressUsageCode(val description: String, val current: Boolean) {
  // PRISON
  CARE("Care or Old Persons Home", true),
  CURFEW("Curfew Order", true),
  DAP("Discharge - Approved Premises", true),
  DBA("Discharge - BASS Accommodation", true),
  DBH("Discharge - Bail/Probation Hostel", true),
  DNF("Discharge - NFA", true),
  DOH("Discharge - Other Hostel", true),
  DPH("Discharge - Permanent Housing", true),
  DSH("Discharge - Supported Housing", true),
  DST("Discharge - Transient/Short Term", true),
  DUT("Discharge - Unknown Accommodation Type", true),
  HDC("HDC Address", true),
  HDC2("2nd HDC Address", true),
  HOME("Home", true),
  HOSP("Hospital", true),
  HOST("Approved Premises", true),
  OTHER("Other", true),
  RECEP("Reception", true),
  RELEASE("Discharge", true),
  RES("Residency Condition", true),
  ROTL("Release on Temporary Licence", true),

  // PROBATION
  A02("Approved Premises", true),
  A16("Awaiting Assessment", true),
  A10("CAS2/BASS accommodation 13 weeks or more", true),
  A11("CAS2/BASS accommodation less than 13 weeks", true),
  A17("CAS3", true),
  A07B("Friends/Family (settled)", true),
  A07A("Friends/Family (transient)", true),
  A14("HOIE Section 10", true),
  A13("HOIE Section 4", true),
  A08A("Homeless - Rough Sleeping", true),
  A08C("Homeless - Shelter/Emergency Hostel/Campsite", true),
  A08("Homeless - Squat", true),
  A01A("Householder (Owner - freehold or leasehold)", true),
  A15("Immigration Detention", true),
  A12("Long Term Residential Healthcare", true),
  A01C("Rental accommodation - private rental", true),
  A01D("Rental accommodation - social rental (LA or other)", true),
  A04("Supported Housing", true),
  A03("Transient/short term accommodation", true),
  A01("Permanent Independent Housing", false),
  A01B("Permanent Independent Housing (LA or private rent)", false),
  A05("No fixed abode", false),
  A06("No information", false),
  A07("Friends/Family", false),
  A08B("Homeless - Other", false),
  A96("Multiple Personal Circumstances Identified", false),
  A97("Unsuitable Accommodation (Migrated Record)", false),
  A98("Suitable Accommodation (Migrated Record)", false),
  A99("Historic Accommodation Record", false),
  CS02("Multi-occupancy housing", false),
  CS03("Partner's home", false),
  CS04("Hospital / Institution", false),
  CS05("Custody", false),
  CS06("Case File Checked Out", false),

  // CPR
  UNKNOWN("Unknown", false);

  companion object {
    fun from(value: String): AddressUsageCode = entries.associateBy { it.name }.getOrDefault(value, UNKNOWN)
  }
}
