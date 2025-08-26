package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.apache.commons.lang3.StringUtils.SPACE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.extentions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
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
  val titleCode: TitleCode? = null,
  val aliases: List<Alias> = emptyList(),
  val masterDefendantId: String? = null,
  val nationalities: List<Nationality> = emptyList(),
  val religion: String? = null,
  val ethnicity: String? = null,
  val ethnicityCode: EthnicityCode? = null,
  val contacts: List<Contact> = emptyList(),
  val addresses: List<Address> = emptyList(),
  val references: List<Reference> = emptyList(),
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
      val contacts: List<Contact> = listOfNotNull(
        Contact.from(ContactType.HOME, probationCase.contactDetails?.telephone),
        Contact.from(ContactType.MOBILE, probationCase.contactDetails?.mobile),
        Contact.from(ContactType.EMAIL, probationCase.contactDetails?.email),
      )
      val references: List<Reference> = listOf(
        Reference(identifierType = IdentifierType.CRO, identifierValue = CROIdentifier.from(probationCase.identifiers.cro).croId),
        Reference(identifierType = IdentifierType.PNC, identifierValue = PNCIdentifier.from(probationCase.identifiers.pnc).pncId),
        Reference(
          identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
          identifierValue = probationCase.identifiers.nationalInsuranceNumber,
        ),
      )
      val nationalities: List<Nationality> = listOf(
        Nationality(NationalityCode.fromProbationMapping(probationCase.nationality?.value)),
      )
      return Person(
        titleCode = TitleCode.from(probationCase.title?.value),
        firstName = probationCase.name.firstName.nullIfBlank(),
        middleNames = probationCase.name.middleNames.nullIfBlank(),
        lastName = probationCase.name.lastName.nullIfBlank(),
        dateOfBirth = probationCase.dateOfBirth,
        crn = probationCase.identifiers.crn,
        ethnicity = probationCase.ethnicity?.value.nullIfBlank(),
        ethnicityCode = EthnicityCode.from(probationCase.ethnicity?.value),
        nationalities = nationalities,
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
      val contacts: List<Contact> = listOfNotNull(
        Contact.from(ContactType.HOME, defendant.personDefendant?.personDetails?.contact?.home),
        Contact.from(ContactType.MOBILE, defendant.personDefendant?.personDetails?.contact?.mobile),
        Contact.from(ContactType.EMAIL, defendant.personDefendant?.personDetails?.contact?.primaryEmail),
      )

      val addresses = listOf(Address.from(defendant.personDefendant?.personDetails?.address))

      val references: List<Reference> = listOfNotNull(
        Reference.from(
          identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
          identifierValue = defendant.personDefendant?.personDetails?.nationalInsuranceNumber,
        ),
        Reference.from(
          identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
          identifierValue = defendant.personDefendant?.driverNumber,
        ),
        Reference.from(
          identifierType = IdentifierType.ARREST_SUMMONS_NUMBER,
          identifierValue = defendant.personDefendant?.arrestSummonsNumber,
        ),
        Reference.from(identifierType = IdentifierType.PNC, identifierValue = defendant.pncId?.pncId),
        Reference.from(identifierType = IdentifierType.CRO, identifierValue = defendant.cro?.croId),
      )

      val nationalities: List<Nationality> = listOf(
        Nationality(NationalityCode.fromCommonPlatformMapping(defendant.personDefendant?.personDetails?.nationalityCode)),
      )

      return Person(
        titleCode = TitleCode.from(defendant.personDefendant?.personDetails?.title.nullIfBlank()),
        firstName = defendant.personDefendant?.personDetails?.firstName.nullIfBlank(),
        lastName = defendant.personDefendant?.personDetails?.lastName.nullIfBlank(),
        middleNames = defendant.personDefendant?.personDetails?.middleName.nullIfBlank(),
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
        ethnicityCode = EthnicityCode.from(defendant.personDefendant?.personDetails?.ethnicity?.selfDefinedEthnicityCode),
        defendantId = defendant.id.nullIfBlank(),
        masterDefendantId = defendant.masterDefendantId.nullIfBlank(),
        contacts = contacts,
        addresses = addresses,
        references = references,
        nationalities = nationalities,
        aliases = defendant.aliases?.map { Alias.from(it) } ?: emptyList(),
        sourceSystem = sourceSystemType,
        sexCode = SexCode.from(defendant.personDefendant?.personDetails),
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      val addresses = listOf(
        Address.from(libraHearingEvent.defendantAddress),
      )
      val references = listOfNotNull(
        Reference.from(identifierType = IdentifierType.CRO, identifierValue = libraHearingEvent.cro?.toString()),
        Reference.from(identifierType = IdentifierType.PNC, identifierValue = libraHearingEvent.pnc?.toString()),
      )
      val nationalities: List<Nationality> = listOf(
        Nationality(NationalityCode.fromLibraMapping(libraHearingEvent.nationality1)),
        Nationality(NationalityCode.fromLibraMapping(libraHearingEvent.nationality2)),
      )
      return Person(
        titleCode = TitleCode.from(libraHearingEvent.name?.title),
        firstName = libraHearingEvent.name?.firstName.nullIfBlank(),
        middleNames = listOfNotNull(libraHearingEvent.name?.forename2.nullIfBlank(), libraHearingEvent.name?.forename3.nullIfBlank()).joinToString(SPACE).trim(),
        lastName = libraHearingEvent.name?.lastName.nullIfBlank(),
        dateOfBirth = libraHearingEvent.dateOfBirth,
        addresses = addresses,
        references = references,
        nationalities = nationalities,
        sourceSystem = LIBRA,
        cId = libraHearingEvent.cId.nullIfBlank(),
        sexCode = SexCode.from(libraHearingEvent),
      )
    }

    fun from(prisoner: Prisoner): Person {
      val emails: List<Contact> = prisoner.emailAddresses.mapNotNull { Contact.from(ContactType.EMAIL, it.email) }
      val phoneNumbers: List<Contact> = listOfNotNull(
        Contact.from(ContactType.HOME, prisoner.getHomePhone()),
        Contact.from(ContactType.MOBILE, prisoner.getMobilePhone()),
      )
      val contacts: List<Contact> = emails + phoneNumbers
      val addresses: List<Address> = Address.fromPrisonerAddressList(prisoner.addresses)
      val references = listOfNotNull(
        Reference.from(identifierType = IdentifierType.CRO, identifierValue = prisoner.cro?.toString()),
        Reference.from(identifierType = IdentifierType.PNC, identifierValue = prisoner.pnc?.toString()),
        Reference.from(
          identifierType = IdentifierType.NATIONAL_INSURANCE_NUMBER,
          identifierValue = prisoner.identifiers.getType("NINO")?.value,
        ),
        Reference.from(
          identifierType = IdentifierType.DRIVER_LICENSE_NUMBER,
          identifierValue = prisoner.identifiers.getType("DL")?.value,
        ),
      )
      val nationalities: List<Nationality> = listOf(
        Nationality(NationalityCode.fromPrisonMapping(prisoner.nationality)),
      )

      return Person(
        prisonNumber = prisoner.prisonNumber.nullIfBlank(),
        titleCode = TitleCode.from(prisoner.title),
        firstName = prisoner.firstName.nullIfBlank(),
        middleNames = prisoner.middleNames.nullIfBlank(),
        lastName = prisoner.lastName.nullIfBlank(),
        dateOfBirth = prisoner.dateOfBirth,
        ethnicity = prisoner.ethnicity.nullIfBlank(),
        ethnicityCode = EthnicityCode.fromPrison(prisoner.ethnicity.nullIfBlank()),
        aliases = prisoner.aliases.map { Alias.from(it) },
        contacts = contacts,
        addresses = addresses,
        references = references,
        sourceSystem = NOMIS,
        nationalities = nationalities,
        religion = prisoner.religion.nullIfBlank(),
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
      aliases = existingPersonEntity.getAliases().map { Alias.from(it) },
      masterDefendantId = existingPersonEntity.masterDefendantId,
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
