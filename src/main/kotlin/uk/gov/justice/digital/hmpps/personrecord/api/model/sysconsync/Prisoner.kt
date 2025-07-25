package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync

data class Prisoner(
  val name: Name,
  val demographicAttributes: DemographicAttributes,
  val religions: List<Religion>,
  val contactInfo: ContactInfo,
  val aliases: List<Alias>,
  val identifiers: List<Identifier>,
  val sentences: List<Sentence>,
)
