package uk.gov.justice.digital.hmpps.personrecord.model.types

enum class ContactType(val description: String) {
  HOME("Home"),
  BUS("Business"),
  FAX("Fax"),
  ALTB("Alternate Business"),
  ALTH("Alternate Home"),
  MOBILE("Mobile"),
  VISIT("Agency Visit Line"),
  EMAIL("Email"),
}
