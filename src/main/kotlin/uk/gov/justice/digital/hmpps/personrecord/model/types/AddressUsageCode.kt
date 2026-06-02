package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class AddressUsageCode(val description: String) {
  // PRISON
  CARE("Care or Old Persons Home"),
  CURFEW("Curfew Order"),
  DAP("Discharge - Approved Premises"),
  DBA("Discharge - BASS Accommodation"),
  DBH("Discharge - Bail/Probation Hostel"),
  DNF("Discharge - NFA"),
  DOH("Discharge - Other Hostel"),
  DPH("Discharge - Permanent Housing"),
  DSH("Discharge - Supported Housing"),
  DST("Discharge - Transient/Short Term"),
  DUT("Discharge - Unknown Accommodation Type"),
  HDC("HDC Address"),
  HDC2("2nd HDC Address"),
  HOME("Home"),
  HOSP("Hospital"),
  HOST("Approved Premises"),
  OTHER("Other"),
  RECEP("Reception"),
  RELEASE("Discharge"),
  RES("Residency Condition"),
  ROTL("Release on Temporary Licence"),

  // PROBATION
  A02("Approved Premises"),
  A16("Awaiting Assessment"),
  A10("CAS2/BASS accommodation 13 weeks or more"),
  A11("CAS2/BASS accommodation less than 13 weeks"),
  A17("CAS3"),
  A07B("Friends/Family (settled)"),
  A07A("Friends/Family (transient)"),
  A14("HOIE Section 10"),
  A13("HOIE Section 4"),
  A08A("Homeless - Rough Sleeping"),
  A08C("Homeless - Shelter/Emergency Hostel/Campsite"),
  A08("Homeless - Squat"),
  A01A("Householder (Owner - freehold or leasehold)"),
  A15("Immigration Detention"),
  A12("Long Term Residential Healthcare"),
  A01C("Rental accommodation - private rental"),
  A01D("Rental accommodation - social rental (LA or other)"),
  A04("Supported Housing"),
  A03("Transient/short term accommodation"),

  // CPR
  UNKNOWN("Unknown"),
  ;

  companion object {
    fun from(value: String): AddressUsageCode = entries.associateBy { it.name }.getOrDefault(value, UNKNOWN)
  }
}
