package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.apache.commons.lang3.StringUtils.SPACE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import java.time.LocalDate
import java.util.UUID
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.Prisoner as SysconPrisoner

data class Person(
  val personId: UUID? = null,
  val firstName: String? = null,
  val middleNames: String? = null,
  val lastName: String? = null,
  val dateOfBirth: LocalDate? = null,
  val dateOfDeath: LocalDate? = null,
  val crn: String? = null,
  var prisonNumber: String? = null,
  var defendantId: String? = null,
  val titleCode: TitleCode? = null,
  val aliases: List<Alias> = emptyList(),
  var masterDefendantId: String? = null,
  var nationalities: List<NationalityCode> = emptyList(),
  var nationalityNotes: String? = null,
  val religion: String? = null,
  val ethnicityCode: EthnicityCode? = null,
  val contacts: List<Contact> = emptyList(),
  var addresses: List<Address> = emptyList(),
  val references: List<Reference> = emptyList(),
  val sourceSystem: SourceSystemType,
  val sentences: List<SentenceInfo> = emptyList(),
  val cId: String? = null,
  val sexCode: SexCode? = null,
  val genderIdentity: GenderIdentityCode? = null,
  val selfDescribedGenderIdentity: String? = null,
  val sexualOrientation: SexualOrientation? = null,
  val disability: Boolean? = null,
  val immigrationStatus: Boolean? = null,
  val birthplace: String? = null,
  val birthCountryCode: String? = null,
  val behaviour: Behaviour = Behaviour(),
) {

  companion object {

    fun List<Reference>.toString(): String = this.joinToString { it.identifierValue.toString() }

    fun from(probationCase: ProbationCase): Person {
      val contacts: List<Contact> = listOfNotNull(
        Contact.from(ContactType.HOME, probationCase.contactDetails?.telephone),
        Contact.from(ContactType.MOBILE, probationCase.contactDetails?.mobile),
        Contact.from(ContactType.EMAIL, probationCase.contactDetails?.email),
      )

      val references = IdentifierType.createProbationIdentifierReferences(probationCase)

      val nationalities: List<NationalityCode> = listOf(
        NationalityCode.fromProbationMapping(probationCase.nationality?.value),
        NationalityCode.fromProbationMapping(probationCase.secondNationality?.value),
      ).mapNotNull { it }

      return Person(
        titleCode = TitleCode.from(probationCase.title?.value),
        firstName = probationCase.name.firstName.nullIfBlank(),
        middleNames = probationCase.name.middleNames.nullIfBlank(),
        lastName = probationCase.name.lastName.nullIfBlank(),
        dateOfBirth = probationCase.dateOfBirth,
        dateOfDeath = probationCase.dateOfDeath,
        crn = probationCase.identifiers.crn,
        ethnicityCode = EthnicityCode.fromProbation(probationCase.ethnicity?.value),
        nationalities = nationalities,
        aliases = probationCase.aliases?.map { Alias.from(it) } ?: emptyList(),
        addresses = Address.fromOffenderAddressList(probationCase.addresses),
        contacts = contacts,
        references = references,
        sourceSystem = DELIUS,
        sentences = probationCase.sentences?.map { SentenceInfo.from(it) } ?: emptyList(),
        sexCode = SexCode.from(probationCase),
        sexualOrientation = SexualOrientation.fromProbation(probationCase),
        religion = probationCase.religion?.value,
        genderIdentity = GenderIdentityCode.from(probationCase),
        selfDescribedGenderIdentity = probationCase.selfDescribedGenderIdentity,
      )
    }

    fun from(defendant: Defendant): Person {
      val contacts: List<Contact> = listOfNotNull(
        Contact.from(ContactType.HOME, defendant.personDefendant?.personDetails?.contact?.home),
        Contact.from(ContactType.MOBILE, defendant.personDefendant?.personDetails?.contact?.mobile),
        Contact.from(ContactType.EMAIL, defendant.personDefendant?.personDetails?.contact?.primaryEmail),
      )

      val addresses = listOfNotNull(Address.from(defendant.personDefendant?.personDetails?.address))

      val references: List<Reference> = listOfNotNull(
        Reference.from(
          identifierType = NATIONAL_INSURANCE_NUMBER,
          identifierValue = defendant.personDefendant?.personDetails?.nationalInsuranceNumber,
        ),
        Reference.from(
          identifierType = DRIVER_LICENSE_NUMBER,
          identifierValue = defendant.personDefendant?.driverNumber,
        ),
        Reference.from(
          identifierType = ARREST_SUMMONS_NUMBER,
          identifierValue = defendant.personDefendant?.arrestSummonsNumber,
        ),
        Reference.from(identifierType = PNC, identifierValue = defendant.pncId?.pncId),
        Reference.from(identifierType = CRO, identifierValue = defendant.cro?.croId),
      )

      val nationalities: List<NationalityCode> = listOf(
        NationalityCode.fromCommonPlatformMapping(defendant.personDefendant?.personDetails?.nationalityCode),
        NationalityCode.fromCommonPlatformMapping(defendant.personDefendant?.personDetails?.additionalNationalityCode),
      ).mapNotNull { it }

      return Person(
        titleCode = TitleCode.from(defendant.personDefendant?.personDetails?.title.nullIfBlank()),
        firstName = defendant.personDefendant?.personDetails?.firstName.nullIfBlank(),
        lastName = defendant.personDefendant?.personDetails?.lastName.nullIfBlank(),
        middleNames = defendant.personDefendant?.personDetails?.middleName.nullIfBlank(),
        dateOfBirth = defendant.personDefendant?.personDetails?.dateOfBirth,
        ethnicityCode = EthnicityCode.fromCommonPlatform(defendant.personDefendant?.personDetails?.ethnicity?.selfDefinedEthnicityCode),
        defendantId = defendant.id.nullIfBlank(),
        masterDefendantId = defendant.masterDefendantId.nullIfBlank(),
        contacts = contacts,
        addresses = addresses,
        references = references,
        nationalities = nationalities,
        aliases = defendant.aliases?.map { Alias.from(it) } ?: emptyList(),
        sourceSystem = COMMON_PLATFORM,
        sexCode = SexCode.from(defendant.personDefendant?.personDetails),
      )
    }

    fun from(libraHearingEvent: LibraHearingEvent): Person {
      val addresses = listOfNotNull(
        Address.from(libraHearingEvent.defendantAddress),
      )
      val references = listOfNotNull(
        Reference.from(identifierType = CRO, identifierValue = libraHearingEvent.cro?.toString()),
        Reference.from(identifierType = PNC, identifierValue = libraHearingEvent.pnc?.toString()),
      )

      return Person(
        titleCode = TitleCode.from(libraHearingEvent.name?.title),
        firstName = libraHearingEvent.name?.firstName.nullIfBlank(),
        middleNames = listOfNotNull(libraHearingEvent.name?.forename2.nullIfBlank(), libraHearingEvent.name?.forename3.nullIfBlank()).joinToString(SPACE).trim(),
        lastName = libraHearingEvent.name?.lastName.nullIfBlank(),
        dateOfBirth = libraHearingEvent.dateOfBirth,
        addresses = addresses,
        references = references,
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
        Reference.from(identifierType = CRO, identifierValue = prisoner.cro?.toString()),
        Reference.from(identifierType = PNC, identifierValue = prisoner.pnc?.toString()),
        Reference.from(
          identifierType = NATIONAL_INSURANCE_NUMBER,
          identifierValue = prisoner.identifiers.getType("NINO")?.value,
        ),
        Reference.from(
          identifierType = DRIVER_LICENSE_NUMBER,
          identifierValue = prisoner.identifiers.getType("DL")?.value,
        ),
      )
      val nationalities: List<NationalityCode> = NationalityCode.fromPrisonMapping(prisoner.nationality)?.let { listOf(it) } ?: emptyList()

      return Person(
        prisonNumber = prisoner.prisonNumber.nullIfBlank(),
        titleCode = TitleCode.from(prisoner.title),
        firstName = prisoner.firstName.nullIfBlank(),
        middleNames = prisoner.middleNames.nullIfBlank(),
        lastName = prisoner.lastName.nullIfBlank(),
        dateOfBirth = prisoner.dateOfBirth,
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

    fun from(prisoner: SysconPrisoner, prisonNumber: String): Person {
      val primaryAlias = prisoner.aliases.firstOrNull { it.isPrimary == true } ?: throw IllegalArgumentException("No primary alias was found for update on prisoner $prisonNumber")

      val identifiers = prisoner.aliases
        .flatMap { it.identifiers?.toList() ?: emptyList() }
        .mapNotNull { Reference.from(IdentifierType.valueOf(it.type.name), it.value) }

      return Person(
        prisonNumber = prisonNumber,
        titleCode = TitleCode.from(primaryAlias.titleCode),
        firstName = primaryAlias.firstName.nullIfBlank(),
        middleNames = primaryAlias.middleNames?.nullIfBlank(),
        lastName = primaryAlias.lastName.nullIfBlank(),
        dateOfBirth = primaryAlias.dateOfBirth,
        ethnicityCode = EthnicityCode.fromPrison(prisoner.demographicAttributes.ethnicityCode),
        aliases = prisoner.aliases.map { Alias.from(it) },
        contacts = prisoner.personContacts.map { contact -> Contact(contact.type, contact.value) },
        addresses = prisoner.addresses.map { Address.from(it) },
        references = identifiers,
        sourceSystem = NOMIS,
        nationalities = listOf(NationalityCode.fromPrisonCode(prisoner.demographicAttributes.nationalityCode)).mapNotNull { it },
        nationalityNotes = prisoner.demographicAttributes.nationalityNote.nullIfBlank(),
        religion = prisoner.demographicAttributes.religionCode.nullIfBlank(),
        sentences = prisoner.sentences.map { SentenceInfo(it.sentenceDate) },
        sexCode = prisoner.demographicAttributes.sexCode,
      )
    }

    fun from(existingPersonEntity: PersonEntity): Person = Person(
      personId = existingPersonEntity.personKey?.personUUID,
      firstName = existingPersonEntity.getPrimaryName().firstName,
      middleNames = existingPersonEntity.getPrimaryName().middleNames,
      lastName = existingPersonEntity.getPrimaryName().lastName,
      dateOfBirth = existingPersonEntity.getPrimaryName().dateOfBirth,
      dateOfDeath = existingPersonEntity.dateOfDeath,
      crn = existingPersonEntity.crn,
      prisonNumber = existingPersonEntity.prisonNumber,
      defendantId = existingPersonEntity.defendantId,
      titleCode = existingPersonEntity.getPrimaryName().titleCode,
      aliases = existingPersonEntity.getAliases().map { Alias.from(it) },
      masterDefendantId = existingPersonEntity.masterDefendantId,
      nationalities = existingPersonEntity.nationalities.map { it.nationalityCode },
      nationalityNotes = existingPersonEntity.nationalityNotes,
      religion = existingPersonEntity.religion,
      ethnicityCode = existingPersonEntity.ethnicityCode,
      contacts = existingPersonEntity.contacts.map { Contact.from(it) },
      addresses = existingPersonEntity.addresses.map { Address.from(it) },
      references = existingPersonEntity.references.map { Reference.from(it) },
      sourceSystem = existingPersonEntity.sourceSystem,
      sentences = existingPersonEntity.sentenceInfo.map { SentenceInfo.from(it) },
      cId = existingPersonEntity.cId,
      sexCode = existingPersonEntity.getPrimaryName().sexCode,
      genderIdentity = existingPersonEntity.genderIdentity,
      selfDescribedGenderIdentity = existingPersonEntity.selfDescribedGenderIdentity,
      sexualOrientation = existingPersonEntity.sexualOrientation,
      disability = existingPersonEntity.disability,
      immigrationStatus = existingPersonEntity.immigrationStatus,
      birthplace = existingPersonEntity.birthplace,
      birthCountryCode = existingPersonEntity.birthCountryCode,
    )
  }

  fun doNotReclusterOnUpdate(): Person {
    this.behaviour.reclusterOnUpdate = false
    return this
  }

  fun doNotLinkOnCreate(): Person {
    this.behaviour.linkOnCreate = false
    return this
  }

  fun isPerson(): Boolean = minimumDataIsPresent()

  private fun minimumDataIsPresent(): Boolean = lastNameIsPresent() && anyOtherPersonalDataIsPresent()

  private fun anyOtherPersonalDataIsPresent() = listOfNotNull(firstName, middleNames, dateOfBirth).joinToString("").isNotEmpty()

  private fun lastNameIsPresent() = lastName?.isNotEmpty() == true
}

data class Behaviour(
  var reclusterOnUpdate: Boolean = true,
  var linkOnCreate: Boolean = true,
)
