package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.apache.commons.lang3.StringUtils.SPACE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDate
import java.util.UUID

data class Person(
  val personId: UUID? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val crn: String? = null,
  var prisonNumber: String? = null,
  var defendantId: String? = null,
  val title: String? = null,
  val aliases: List<Alias> = emptyList(),
  val masterDefendantId: String? = null,
  val nationality: String? = null,
  val religion: String? = null,
  val ethnicity: String? = null,
  val contacts: List<Contact> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val references: List<Reference> = emptyList(),
  var selfMatchScore: Double? = null,
  val sourceSystem: SourceSystemType,
  val sentences: List<SentenceInfo> = emptyList(),
  val cId: String? = null,
  val sexCode: SexCode? = null,
  var reclusterOnUpdate: Boolean = true,
  var linkOnCreate: Boolean = true,
) {

  companion object {

    fun List<Reference>.getType(type: IdentifierType): List<Reference> = this.filter { it.identifierType == type }

    fun List<Reference>.toString(): String = this.joinToString { it.identifierValue.toString() }

    fun from(probationCase: ProbationCase): Person {
      val contacts: List<Contact> = listOf(
        Contact.from(ContactType.HOME, probationCase.contactDetails?.telephone),
        Contact.from(ContactType.MOBILE, probationCase.contactDetails?.mobile),
        Contact.from(ContactType.EMAIL, probationCase.contactDetails?.email),
      )
      val references: List<Reference> = listOf(
        Reference(identifierType = IdentifierType.CRO, identifierValue = probationCase.identifiers.cro?.croId),
        Reference(identifierType = IdentifierType.PNC, identifierValue = probationCase.identifiers.pnc?.pncId),
        Reference(
          identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
          identifierValue = probationCase.identifiers.nationalInsuranceNumber,
        ),
      )
      return Person(
        title = probationCase.title?.value,
        firstName = probationCase.name.firstName,
        middleNames = probationCase.name.middleNames,
        lastName = probationCase.name.lastName,
        dateOfBirth = probationCase.dateOfBirth,
        crn = probationCase.identifiers.crn,
        ethnicity = probationCase.ethnicity?.value,
        nationality = probationCase.nationality?.value,
        aliases = probationCase.aliases?.map { Alias.from(it) } ?: emptyList(),
        addresses = Address.fromOffenderAddressList(probationCase.addresses),
        contacts = contacts,
        references = references,
        sourceSystem = DELIUS,
        sentences = probationCase.sentences?.map { SentenceInfo.from(it) } ?: emptyList(),
        sexCode = SexCode.from(probationCase),
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
            buildingName = defendant.personDefendant?.personDetails?.address?.address1,
            buildingNumber = defendant.personDefendant?.personDetails?.address?.address2,
            thoroughfareName = defendant.personDefendant?.personDetails?.address?.address3,
            dependentLocality = defendant.personDefendant?.personDetails?.address?.address4,
            postTown = defendant.personDefendant?.personDetails?.address?.address5,

          ),
        )
      }

      val references: List<Reference> = listOf(
        Reference(
          identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
          identifierValue = defendant.personDefendant?.personDetails?.nationalInsuranceNumber,
        ),
        Reference(
          identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
          identifierValue = defendant.personDefendant?.driverNumber,
        ),
        Reference(
          identifierType = IdentifierType.ARREST_SUMMONS_NUMBER,
          identifierValue = defendant.personDefendant?.arrestSummonsNumber,
        ),
        Reference(identifierType = IdentifierType.PNC, identifierValue = defendant.pncId?.pncId),
        Reference(identifierType = IdentifierType.CRO, identifierValue = defendant.cro?.croId),
      )

      return Person(
        firstName = defendant.personDefendant?.personDetails?.firstName,
        lastName = defendant.personDefendant?.personDetails?.lastName,
        middleNames = defendant.personDefendant?.personDetails?.middleName,
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
        defendantId = defendant.id,
        masterDefendantId = defendant.masterDefendantId,
        contacts = contacts,
        addresses = addresses,
        references = references,
        aliases = defendant.aliases?.map { Alias.from(it) } ?: emptyList(),
        sourceSystem = sourceSystemType,
        sexCode = SexCode.from(defendant.personDefendant?.personDetails),
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      val addresses = listOf(
        Address(
          postcode = libraHearingEvent.defendantAddress?.postcode,
          buildingName = libraHearingEvent.defendantAddress?.buildingName,
          buildingNumber = libraHearingEvent.defendantAddress?.buildingNumber,
          thoroughfareName = libraHearingEvent.defendantAddress?.thoroughfareName,
          dependentLocality = libraHearingEvent.defendantAddress?.dependentLocality,
          postTown = libraHearingEvent.defendantAddress?.postTown,
        ),
      )
      val references = listOf(
        Reference(identifierType = IdentifierType.CRO, identifierValue = libraHearingEvent.cro?.toString()),
        Reference(identifierType = IdentifierType.PNC, identifierValue = libraHearingEvent.pnc?.toString()),
      )
      return Person(
        title = libraHearingEvent.name?.title,
        firstName = libraHearingEvent.name?.firstName,
        middleNames = listOfNotNull(libraHearingEvent.name?.forename2, libraHearingEvent.name?.forename3).joinToString(SPACE).trim(),
        lastName = libraHearingEvent.name?.lastName,
        dateOfBirth = libraHearingEvent.dateOfBirth,
        addresses = addresses,
        references = references,
        sourceSystem = LIBRA,
        cId = libraHearingEvent.cId,
        sexCode = SexCode.from(libraHearingEvent),
      )
    }

    fun from(prisoner: Prisoner): Person {
      val emails: List<Contact> = prisoner.emailAddresses.map { Contact.from(ContactType.EMAIL, it.email) }
      val phoneNumbers: List<Contact> = listOf(
        Contact.from(ContactType.HOME, prisoner.getHomePhone()),
        Contact.from(ContactType.MOBILE, prisoner.getMobilePhone()),
      )
      val contacts: List<Contact> = emails + phoneNumbers
      val addresses: List<Address> = Address.fromPrisonerAddressList(prisoner.addresses)
      val references = listOf(
        Reference(identifierType = IdentifierType.CRO, identifierValue = prisoner.cro?.toString()),
        Reference(identifierType = IdentifierType.PNC, identifierValue = prisoner.pnc?.toString()),
        Reference(
          identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
          identifierValue = prisoner.identifiers.getType("NINO")?.value,
        ),
        Reference(
          identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
          identifierValue = prisoner.identifiers.getType("DL")?.value,
        ),

      )

      return Person(
        prisonNumber = prisoner.prisonNumber,
        title = prisoner.title,
        firstName = prisoner.firstName,
        middleNames = prisoner.middleNames,
        lastName = prisoner.lastName,
        dateOfBirth = prisoner.dateOfBirth,
        ethnicity = prisoner.ethnicity,
        aliases = prisoner.aliases.map { Alias.from(it) },
        contacts = contacts,
        addresses = addresses,
        references = references,
        sourceSystem = NOMIS,
        nationality = prisoner.nationality,
        religion = prisoner.religion,
        sentences = prisoner.allConvictedOffences?.map { SentenceInfo.from(it) } ?: emptyList(),
        sexCode = SexCode.from(prisoner),
      )
    }

    fun from(existingPersonEntity: PersonEntity): Person = Person(
      personId = existingPersonEntity.personKey?.personUUID,
      firstName = existingPersonEntity.getPrimaryName().firstName,
      middleNames = existingPersonEntity.getPrimaryName().middleNames,
      lastName = existingPersonEntity.getPrimaryName().lastName,
      dateOfBirth = existingPersonEntity.getPrimaryName().dateOfBirth,
      crn = existingPersonEntity.crn,
      prisonNumber = existingPersonEntity.prisonNumber,
      defendantId = existingPersonEntity.defendantId,
      title = existingPersonEntity.getPrimaryName().title,
      aliases = existingPersonEntity.getAliases().map { Alias.from(it) },
      masterDefendantId = existingPersonEntity.masterDefendantId,
      nationality = existingPersonEntity.nationality,
      religion = existingPersonEntity.religion,
      ethnicity = existingPersonEntity.ethnicity,
      contacts = existingPersonEntity.contacts.map { Contact.convertEntityToContact(it) },
      addresses = existingPersonEntity.addresses.map { Address.from(it) },
      references = existingPersonEntity.references.map { Reference.from(it) },
      sourceSystem = existingPersonEntity.sourceSystem,
      sentences = existingPersonEntity.sentenceInfo.map { SentenceInfo.from(it) },
      sexCode = existingPersonEntity.sexCode,
    )
  }

  fun doNotReclusterOnUpdate(): Person {
    reclusterOnUpdate = false
    return this
  }
  fun doNotLinkOnCreate(): Person {
    linkOnCreate = false
    return this
  }
  fun isPerson(): Boolean = minimumDataIsPresent()

  private fun minimumDataIsPresent(): Boolean = lastNameIsPresent() && anyOtherPersonalDataIsPresent()

  private fun anyOtherPersonalDataIsPresent() = listOfNotNull(firstName, middleNames, dateOfBirth).joinToString("").isNotEmpty()

  private fun lastNameIsPresent() = lastName?.isNotEmpty() == true
}
