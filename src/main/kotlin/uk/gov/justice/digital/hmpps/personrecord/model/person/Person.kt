package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
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
  val masterDefendantId: String? = null,
  val contacts: List<Contact> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val references: List<Reference> = emptyList(),
  val sourceSystemType: SourceSystemType,
  val nationalInsuranceNumber: String? = null,
  val birthplace: String? = null,
  val birthCountry: String? = null,
  val nationality: String? = null,
  val religion: String? = null,
  val sexualOrientation: String? = null,
  val sex: String? = null,

) {
  companion object {

    fun from(person: Person): PersonKeyEntity {
      return PersonKeyEntity(
        personId = person.personId,
      )
    }

    fun from(probationCase: ProbationCase): Person {
      val contacts: List<Contact> = listOf(
        Contact.from(ContactType.HOME, probationCase.contactDetails?.telephone),
        Contact.from(ContactType.MOBILE, probationCase.contactDetails?.mobile),
        Contact.from(ContactType.EMAIL, probationCase.contactDetails?.email),
      )
      val references: List<Reference> = listOf(
        Reference.from(IdentifierType.CRO, probationCase.identifiers.cro?.croId),
        Reference.from(IdentifierType.PNC, probationCase.identifiers.pnc?.pncId),
        Reference.from(IdentifierType.NATIONAL_INSURANCE_NUMBER, probationCase.identifiers.nationalInsuranceNumber),
      )
      return Person(
        title = probationCase.title?.value,
        firstName = probationCase.name.firstName,
        middleNames = probationCase.name.middleNames?.split(" ") ?: emptyList(),
        lastName = probationCase.name.lastName,
        dateOfBirth = probationCase.dateOfBirth,
        otherIdentifiers = OtherIdentifiers(
          crn = probationCase.identifiers.crn,
        ),
        aliases = probationCase.aliases?.map { Alias.from(it) } ?: emptyList(),
        addresses = probationCase.addresses.map { Address(it.postcode) },
        contacts = contacts,
        references = references,
        sourceSystemType = DELIUS,
      )
    }

    fun from(defendant: Defendant, sourceSystemType: SourceSystemType = COMMON_PLATFORM): Person {
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

      val references: List<Reference> = listOf(
        Reference.from(IdentifierType.NATIONAL_INSURANCE_NUMBER, defendant.personDefendant?.personDetails?.nationalInsuranceNumber),
        Reference.from(IdentifierType.DRIVER_LICENSE_NUMBER, defendant.personDefendant?.driverNumber),
        Reference.from(IdentifierType.ARREST_SUMMONS_NUMBER, defendant.personDefendant?.arrestSummonsNumber),
        Reference.from(IdentifierType.PNC, defendant.pncId?.pncId),
        Reference.from(IdentifierType.CRO, defendant.cro?.croId),
      )

      return Person(
        firstName = defendant.personDefendant?.personDetails?.firstName,
        lastName = defendant.personDefendant?.personDetails?.lastName,
        middleNames = defendant.personDefendant?.personDetails?.middleName?.split(" "),
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
        defendantId = defendant.id,
        masterDefendantId = defendant.masterDefendantId,
        contacts = contacts,
        addresses = addresses,
        references = references,
        aliases = defendant.aliases?.map { Alias.from(it) } ?: emptyList(),
        sourceSystemType = sourceSystemType,
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      val addresses = listOf(Address(libraHearingEvent.defendantAddress?.postcode))
      val references = listOf(
        Reference.from(IdentifierType.CRO, libraHearingEvent.cro?.toString()),
        Reference.from(IdentifierType.PNC, libraHearingEvent.pnc?.toString()),
      )
      return Person(
        title = libraHearingEvent.name?.title,
        firstName = libraHearingEvent.name?.firstName,
        lastName = libraHearingEvent.name?.lastName,
        dateOfBirth = libraHearingEvent.dateOfBirth,
        addresses = addresses,
        references = references,
        sourceSystemType = LIBRA,
      )
    }

    fun from(prisoner: Prisoner): Person {
      val emails: List<Contact> = prisoner.emailAddresses.map { Contact.from(ContactType.EMAIL, it.email) }
      val phoneNumbers: List<Contact> = listOf(
        Contact.from(ContactType.HOME, prisoner.getHomePhone()),
        Contact.from(ContactType.MOBILE, prisoner.getMobilePhone()),
      )
      val contacts: List<Contact> = emails + phoneNumbers
      val addresses: List<Address> = prisoner.addresses.map { Address(it.postcode, it.fullAddress) }
      val references = listOf(
        Reference.from(IdentifierType.CRO, prisoner.cro?.toString()),
        Reference.from(IdentifierType.PNC, prisoner.pnc?.toString()),
        Reference.from(IdentifierType.NATIONAL_INSURANCE_NUMBER, prisoner.nationalInsuranceNumber?.toString()),
        Reference.from(IdentifierType.DRIVER_LICENSE_NUMBER, prisoner.driverLicenseNumber?.toString()),
      )

      return Person(
        otherIdentifiers = OtherIdentifiers(
          prisonNumber = prisoner.prisonNumber,
        ),
        title = prisoner.title,
        firstName = prisoner.firstName,
        middleNames = prisoner.middleNames?.split(" ") ?: emptyList(),
        lastName = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
        aliases = prisoner.aliases.map { Alias.from(it) } ?: emptyList(),
        contacts = contacts,
        addresses = addresses,
        references = references,
        sourceSystemType = NOMIS,
        birthplace = prisoner.birthplace,
        birthCountry = prisoner.birthCountry,
        nationality = prisoner.nationality,
        religion = prisoner.religion,
        sexualOrientation = prisoner.sexualOrientation,
        sex = prisoner.sex,
      )
    }
  }
}
