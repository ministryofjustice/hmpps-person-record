package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.apache.commons.lang3.StringUtils.SPACE
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform.Defendant
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Prisoner.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.extensions.nullIfBlank
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.AAMR
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ACC
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.AI02
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.AMRL
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.APNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ARREST_SUMMONS_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.ASN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.CRO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DNOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DOFF
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRIVER_LICENSE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.DRL
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.IMMN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.LBCN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.LCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.LIFN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.MFCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.MSVN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.MTCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NATIONAL_INSURANCE_NUMBER
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NHS
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NINO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.NPNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.OTHR
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PARN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PCRN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PRNOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.PST
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.SPNC
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.URN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.VISO
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.XIMMN
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.XNOMS
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType.YCRN
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
  val nationalities: List<NationalityCode> = emptyList(),
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
      val references = createReferences(probationCase)

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

    private fun createReferences(probationCase: ProbationCase): List<Reference> = listOf(
      Reference(identifierType = CRO, identifierValue = CROIdentifier.from(probationCase.identifiers.cro).croId),
      Reference(identifierType = PNC, identifierValue = PNCIdentifier.from(probationCase.identifiers.pnc).pncId),
      Reference(
        identifierType = NATIONAL_INSURANCE_NUMBER,
        identifierValue = probationCase.identifiers.nationalInsuranceNumber,
      ),
      createReferenceForAdditionalIdentifier("AAMR", AAMR, probationCase),
      createReferenceForAdditionalIdentifier("ACC", ACC, probationCase),
      createReferenceForAdditionalIdentifier("APNC", APNC, probationCase),
      createReferenceForAdditionalIdentifier("AMRL", AMRL, probationCase),
      createReferenceForAdditionalIdentifier("ASN", ASN, probationCase),
      createReferenceForAdditionalIdentifier("URN", URN, probationCase),
      createReferenceForAdditionalIdentifier("DRL", DRL, probationCase),
      createReferenceForAdditionalIdentifier("DNOMS", DNOMS, probationCase),
      createReferenceForAdditionalIdentifier("XIMMN", XIMMN, probationCase),
      createReferenceForAdditionalIdentifier("XNOMS", XNOMS, probationCase),
      createReferenceForAdditionalIdentifier("IMMN", IMMN, probationCase),
      createReferenceForAdditionalIdentifier("DOFF", DOFF, probationCase),
      createReferenceForAdditionalIdentifier("LCRN", LCRN, probationCase),
      createReferenceForAdditionalIdentifier("LBCN", LBCN, probationCase),
      createReferenceForAdditionalIdentifier("LIFN", LIFN, probationCase),
      createReferenceForAdditionalIdentifier("MFCRN", MFCRN, probationCase),
      createReferenceForAdditionalIdentifier("MTCRN", MTCRN, probationCase),
      createReferenceForAdditionalIdentifier("MSVN", MSVN, probationCase),
      createReferenceForAdditionalIdentifier("NINO", NINO, probationCase),
      createReferenceForAdditionalIdentifier("NHS", NHS, probationCase),
      createReferenceForAdditionalIdentifier("NOMS", NOMS, probationCase),
      createReferenceForAdditionalIdentifier("OTHR", OTHR, probationCase),
      createReferenceForAdditionalIdentifier("PCRN", PCRN, probationCase),
      createReferenceForAdditionalIdentifier("PARN", PARN, probationCase),
      createReferenceForAdditionalIdentifier("PST", PST, probationCase),
      createReferenceForAdditionalIdentifier("AI02", AI02, probationCase),
      createReferenceForAdditionalIdentifier("PRNOMS", PRNOMS, probationCase),
      createReferenceForAdditionalIdentifier("SPNC", SPNC, probationCase),
      createReferenceForAdditionalIdentifier("NPNC", NPNC, probationCase),
      createReferenceForAdditionalIdentifier("VISO", VISO, probationCase),
      createReferenceForAdditionalIdentifier("YCRN", YCRN, probationCase),
    )

    private fun createReferenceForAdditionalIdentifier(
      id: String,
      identifier: IdentifierType,
      probationCase: ProbationCase,
    ): Reference = Reference(
      identifierType = identifier,
      identifierValue = probationCase.identifiers.additionalIdentifiers?.filter {
        it.type?.value == id
      }?.firstNotNullOfOrNull { it.value },
    )

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

    fun from(existingPersonEntity: PersonEntity): Person = Person(
      personId = existingPersonEntity.personKey?.personUUID,
      firstName = existingPersonEntity.getPrimaryName().firstName,
      middleNames = existingPersonEntity.getPrimaryName().middleNames,
      lastName = existingPersonEntity.getPrimaryName().lastName,
      dateOfBirth = existingPersonEntity.getPrimaryName().dateOfBirth,
      sexCode = existingPersonEntity.getPrimaryName().sexCode,
      crn = existingPersonEntity.crn,
      prisonNumber = existingPersonEntity.prisonNumber,
      defendantId = existingPersonEntity.defendantId,
      aliases = existingPersonEntity.getAliases().map { Alias.from(it) },
      masterDefendantId = existingPersonEntity.masterDefendantId,
      religion = existingPersonEntity.religion,
      contacts = existingPersonEntity.contacts.map { Contact.convertEntityToContact(it) },
      addresses = existingPersonEntity.addresses.map { Address.from(it) },
      references = existingPersonEntity.references.map { Reference.from(it) },
      sourceSystem = existingPersonEntity.sourceSystem,
      sentences = existingPersonEntity.sentenceInfo.map { SentenceInfo.from(it) },
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
