package uk.gov.justice.digital.hmpps.personrecord.model.person

import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.NationalityEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PseudonymEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.EthnicityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.GenderIdentityCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.TitleCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.nationality.NationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomBoolean
import uk.gov.justice.digital.hmpps.personrecord.test.randomBuildingNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomDefendantId
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomNationalityCode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonEthnicity
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonSexualOrientation
import uk.gov.justice.digital.hmpps.personrecord.test.randomReligion
import uk.gov.justice.digital.hmpps.personrecord.test.randomTitleCode
import java.util.UUID.randomUUID
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

class PersonEntityMappingTest {

  @Test
  fun `should map all dto fields from entity`() {
    val person = Person.from(createPersonEntity())
    val personProps = Person::class.memberProperties.filterNot {
      it.name == "behaviour"
    }

    assertValues(person, personProps)
  }

  @Test
  fun `should update all entity fields from dto`() {
    // PersonEntity.new calls update which sets values from Person
    val entity = PersonEntity.new(createPerson())
    val entityProps = PersonEntity::class.memberProperties.filterNot {
      it.name == "id" ||
        it.name == "overrideMarker" ||
        it.name == "overrideScopes" ||
        it.name == "mergedTo" ||
        it.name == "matchId" ||
        it.name == "lastModified" ||
        it.name == "version" ||
        it.name == "personKey"
    }

    assertValues(entity, entityProps)
  }

  private fun <T : Any> assertValues(target: T, props: Collection<KProperty1<T, *>>) {
    val softly = SoftAssertions()

    props.forEach { prop ->
      prop.isAccessible = true
      val value = prop.get(target)
      val isCollection = prop.returnType.jvmErasure.isSubclassOf(Collection::class)

      softly.assertThat(value)
        .withFailMessage {
          if (isCollection) {
            "Expected ${target::class.simpleName}.${prop.name} to have at least 1 element, but was empty"
          } else {
            "Expected ${target::class.simpleName}.${prop.name} to be set, but was null"
          }
        }
        .matches { v ->
          when {
            isCollection -> v is Collection<*> && v.isNotEmpty()
            else -> v != null
          }
        }
    }

    softly.assertAll()
  }

  private fun createPersonEntity(): PersonEntity = PersonEntity(
    personKey = PersonKeyEntity(personUUID = randomUUID()),
    pseudonyms = mutableListOf(
      PseudonymEntity(nameType = NameType.PRIMARY, firstName = randomName(), lastName = randomName(), middleNames = randomName(), titleCode = randomTitleCode().value, dateOfBirth = randomDate(), sexCode = SexCode.F),
      PseudonymEntity(nameType = NameType.ALIAS, firstName = randomName(), lastName = randomName(), middleNames = randomName(), titleCode = randomTitleCode().value, dateOfBirth = randomDate(), sexCode = SexCode.M),
    ),
    addresses = mutableListOf(AddressEntity(buildingNumber = randomBuildingNumber(), postcode = randomPostcode(), recordType = AddressRecordType.PRIMARY)),
    contacts = mutableListOf(ContactEntity(contactType = ContactType.HOME, contactValue = randomName())),
    references = mutableListOf(ReferenceEntity(identifierType = IdentifierType.IMMN, identifierValue = randomName())),
    sentenceInfo = mutableListOf(SentenceInfoEntity(sentenceDate = randomDate())),
    crn = randomCrn(),
    defendantId = randomDefendantId(),
    masterDefendantId = randomDefendantId(),
    prisonNumber = randomPrisonNumber(),
    birthplace = randomName(),
    birthCountryCode = randomName(),
    nationalities = mutableListOf(NationalityEntity(nationalityCode = NationalityCode.CDR)),
    religion = randomReligion(),
    sexualOrientation = SexualOrientation.fromPrison(randomPrisonSexualOrientation().key),
    ethnicityCode = EthnicityCode.fromPrison(randomPrisonEthnicity()),
    genderIdentity = GenderIdentityCode.GISD,
    selfDescribedGenderIdentity = randomName(),
    disability = randomBoolean(),
    dateOfDeath = randomDate(),
    sourceSystem = SourceSystemType.NOMIS,
    cId = randomCId(),
    immigrationStatus = randomBoolean(),

    // not part of the mapping, but is required
    matchId = randomUUID(),
  )

  private fun createPerson(): Person = Person(
    personId = randomUUID(),
    firstName = randomName(),
    middleNames = randomName(),
    lastName = randomName(),
    dateOfBirth = randomDate(),
    dateOfDeath = randomDate(),
    crn = randomCrn(),
    prisonNumber = randomPrisonNumber(),
    defendantId = randomDefendantId(),
    titleCode = TitleCode.MISS,
    aliases = listOf(Alias(firstName = randomName(), middleNames = randomName(), lastName = randomName(), dateOfBirth = randomDate(), titleCode = TitleCode.MISS, sexCode = SexCode.N)),
    masterDefendantId = randomDefendantId(),
    nationalities = listOf(randomNationalityCode()),
    religion = randomReligion(),
    ethnicityCode = EthnicityCode.O1,
    contacts = listOf(Contact(contactType = ContactType.HOME, contactValue = randomName())),
    addresses = listOf(Address(noFixedAbode = randomBoolean(), startDate = randomDate(), endDate = randomDate(), postcode = randomPostcode(), buildingName = randomName(), buildingNumber = randomBuildingNumber(), thoroughfareName = randomName(), dependentLocality = randomName(), postTown = randomName())),
    references = listOf(Reference(identifierType = IdentifierType.PNC, identifierValue = randomName())),
    sourceSystem = SourceSystemType.NOMIS,
    sentences = listOf(SentenceInfo.from(SentenceInfoEntity(sentenceDate = randomDate()))),
    cId = randomCId(),
    sexCode = SexCode.M,
    genderIdentity = GenderIdentityCode.GIM,
    selfDescribedGenderIdentity = randomName(),
    sexualOrientation = SexualOrientation.HET,
    disability = randomBoolean(),
    immigrationStatus = randomBoolean(),
    birthplace = randomName(),
    birthCountryCode = randomName(),
  )
}
