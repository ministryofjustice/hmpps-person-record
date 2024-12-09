package uk.gov.justice.digital.hmpps.personrecord.api.model

import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.person.Alias
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.person.Reference
import uk.gov.justice.digital.hmpps.personrecord.model.person.SentenceInfo
import java.time.LocalDate

// no references to model.person here please
data class SysconSyncPrisoner(
  // TODO copied from Person :-)
  val firstName: String? = null,
  val middleNames: List<String>? = emptyList(),
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val crn: String? = null,
  var prisonNumber: String? = null,
  val title: String? = null,
  val aliases: List<Alias> = emptyList(),
  val masterDefendantId: String? = null,
  val nationality: String? = null,
  val religion: String? = null,
  val ethnicity: String? = null,
  val contacts: List<Contact> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val references: List<Reference> = emptyList(),
  val sentences: List<SentenceInfo> = emptyList(),
  val currentlyManaged: Boolean? = null,
)
