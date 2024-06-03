package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonIdentifierEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.HMCTS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDate
import java.util.*

data class Person(
  val personId: UUID? = null,
  val firstName: String? = null,
  val middleNames: List<String>? = emptyList(),
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val otherIdentifiers: OtherIdentifiers? = null,
  val defendantId: String? = null,
  val title: String? = null,
  val aliases: List<Alias> = emptyList(),
  val driverLicenseNumber: String? = null,
  val arrestSummonsNumber: String? = null,
  val masterDefendantId: String? = null,
  val nationalInsuranceNumber: String? = null,
  val contacts: List<Contact> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val sourceSystemType: SourceSystemType,
) {
  companion object {

    fun from(person: Person): PersonIdentifierEntity {
      return PersonIdentifierEntity(
        personId = person.personId,
      )
    }

    fun from(probationCase: ProbationCase): Person {
      val contacts: List<Contact> = listOf(
        Contact.from(ContactType.HOME, probationCase.contactDetails?.telephone),
        Contact.from(ContactType.MOBILE, probationCase.contactDetails?.mobile),
        Contact.from(ContactType.EMAIL, probationCase.contactDetails?.email),
      )
      return Person(
        title = probationCase.title?.value,
        firstName = probationCase.name.firstName,
        middleNames = probationCase.name.middleNames?.split(" ") ?: emptyList(),
        lastName = probationCase.name.lastName,
        dateOfBirth = probationCase.dateOfBirth,
        otherIdentifiers = OtherIdentifiers(
          crn = probationCase.identifiers.crn,
          pncIdentifier = probationCase.identifiers.pnc,
          croIdentifier = probationCase.identifiers.cro,
          prisonNumber = probationCase.identifiers.prisonNumber,
        ),
        nationalInsuranceNumber = probationCase.identifiers.nationalInsuranceNumber,
        aliases = probationCase.aliases?.map { Alias.from(it) } ?: emptyList(),
        addresses = probationCase.addresses.map { Address(it.postcode) },
        contacts = contacts,
        sourceSystemType = DELIUS,
      )
    }

    fun from(defendant: Defendant): Person {
      val contacts: List<Contact> = listOf(
        Contact.from(ContactType.HOME, defendant.personDefendant?.personDetails?.contact?.home),
        Contact.from(ContactType.MOBILE, defendant.personDefendant?.personDetails?.contact?.mobile),
        Contact.from(ContactType.EMAIL, defendant.personDefendant?.personDetails?.contact?.primaryEmail),
      )

      val addresses: MutableList<Address> = mutableListOf()
      defendant.personDefendant?.personDetails?.address?.postcode.let {
        addresses.add(
          Address(
            postcode = defendant.personDefendant?.personDetails?.address?.postcode,
          ),
        )
      }

      return Person(
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = defendant.pncId,
          croIdentifier = defendant.cro,
        ),
        firstName = defendant.personDefendant?.personDetails?.firstName,
        lastName = defendant.personDefendant?.personDetails?.lastName,
        middleNames = defendant.personDefendant?.personDetails?.middleName?.split(" "),
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
        driverLicenseNumber = defendant.personDefendant?.driverNumber,
        arrestSummonsNumber = defendant.personDefendant?.arrestSummonsNumber,
        defendantId = defendant.id,
        masterDefendantId = defendant.masterDefendantId,
        nationalInsuranceNumber = defendant.personDefendant?.personDetails?.nationalInsuranceNumber,
        contacts = contacts,
        addresses = addresses,
        aliases = defendant.aliases?.map { Alias.from(it) } ?: emptyList(),
        sourceSystemType = HMCTS,
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      val addresses = listOf(Address(libraHearingEvent.defendantAddress?.postcode))
      return Person(
        title = libraHearingEvent.name?.title,
        otherIdentifiers = OtherIdentifiers(
          pncIdentifier = libraHearingEvent.pnc,
          croIdentifier = libraHearingEvent.cro,
        ),
        firstName = libraHearingEvent.name?.firstName,
        lastName = libraHearingEvent.name?.lastName,
        dateOfBirth = libraHearingEvent.dateOfBirth,
        addresses = addresses,
        sourceSystemType = HMCTS,
      )
    }

    fun from(prisoner: Prisoner): Person {
      val emails: List<Contact> = prisoner.emailAddresses.map { Contact.from(ContactType.EMAIL, it.email) }
      val phoneNumbers: List<Contact> = listOf(
        Contact.from(ContactType.HOME, prisoner.getHomePhone()),
        Contact.from(ContactType.MOBILE, prisoner.getMobilePhone()),
      )
      val contacts: List<Contact> = emails + phoneNumbers
      val addresses: List<Address> = prisoner.addresses.map { Address(it.postcode) }

      return Person(
        otherIdentifiers = OtherIdentifiers(
          prisonNumber = prisoner.prisonNumber,
          pncIdentifier = prisoner.pnc,
          croIdentifier = prisoner.cro,
        ),
        title = prisoner.title,
        firstName = prisoner.firstName,
        middleNames = prisoner.middleNames?.split(" ") ?: emptyList(),
        lastName = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
        aliases = prisoner.aliases.map { Alias.from(it) } ?: emptyList(),
        contacts = contacts,
        addresses = addresses,
        sourceSystemType = NOMIS,
      )
    }
  }
}
