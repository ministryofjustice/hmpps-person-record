package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDate

@Entity
@Table(name = "person")
class PersonEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @Column
  var title: String? = null,

  @Column(name = "first_name")
  var firstName: String? = null,

  @Column(name = "last_name")
  var lastName: String? = null,

  @Column(name = "middle_names")
  var middleNames: String? = null,

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fk_person_key_id", referencedColumnName = "id", nullable = true)
  var personKey: PersonKeyEntity? = null,

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var pseudonyms: MutableList<PseudonymEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var addresses: MutableList<AddressEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var contacts: MutableList<ContactEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var references: MutableList<ReferenceEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var sentenceInfo: MutableList<SentenceInfoEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var overrideMarkers: MutableList<OverrideMarkerEntity> = mutableListOf(),

  @Column
  var crn: String? = null,

  @Column(name = "defendant_id")
  var defendantId: String? = null,

  @Column(name = "master_defendant_id")
  var masterDefendantId: String? = null,

  @Column(name = "prison_number")
  var prisonNumber: String? = null,

  @Column(name = "birth_place")
  val birthplace: String? = null,

  @Column(name = "birth_country")
  val birthCountry: String? = null,

  @Column
  var nationality: String? = null,

  @Column
  var religion: String? = null,

  @Column(name = "sexual_orientation")
  val sexualOrientation: String? = null,

  @Column(name = "date_of_birth")
  var dateOfBirth: LocalDate? = null,

  @Column
  val sex: String? = null,

  @Column
  val ethnicity: String? = null,

  @Column(name = "currently_managed")
  var currentlyManaged: Boolean? = null,

  @Column(name = "merged_to")
  var mergedTo: Long? = null,

  @Column
  @Enumerated(STRING)
  val sourceSystem: SourceSystemType,

  @Version
  var version: Int = 0,

) {

  fun addExcludeOverrideMarker(excludeRecord: PersonEntity) {
    this.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = excludeRecord.id, person = this),
    )
  }

  fun removeMergedLink() {
    this.mergedTo = null
  }

  fun removePersonKeyLink() {
    this.personKey?.personEntities?.remove(this)
    this.personKey = null
  }

  fun getIdentifiersForMatching(identifiers: List<IdentifierType>): List<ReferenceEntity> {
    return this.references.filter { identifiers.contains(it.identifierType) && !it.identifierValue.isNullOrEmpty() }
  }

  fun update(person: Person) {
    this.title = person.title
    this.firstName = person.firstName
    this.middleNames = person.middleNames?.joinToString(" ") { it }
    this.lastName = person.lastName
    this.dateOfBirth = person.dateOfBirth
    this.defendantId = person.defendantId
    this.crn = person.crn
    this.prisonNumber = person.prisonNumber
    this.masterDefendantId = person.masterDefendantId
    this.nationality = person.nationality
    this.religion = person.religion
    updateChildEntities(person)
  }

  private fun updateChildEntities(person: Person) {
    pseudonyms.clear()
    addresses.clear()
    contacts.clear()
    references.clear()
    sentenceInfo.clear()
    updatePersonAddresses(person)
    updatePersonContacts(person)
    updatePersonAliases(person)
    updatePersonReferences(person)
    updatePersonSentences(person)
  }

  private fun updatePersonAddresses(person: Person) {
    val personAddresses = AddressEntity.fromList(person.addresses)
    personAddresses.forEach { personAddressEntity -> personAddressEntity.person = this }
    this.addresses.addAll(personAddresses)
  }

  private fun updatePersonAliases(person: Person) {
    val personAliases = PseudonymEntity.fromList(person.aliases)
    personAliases.forEach { personAliasEntity -> personAliasEntity.person = this }
    this.pseudonyms.addAll(personAliases)
  }

  private fun updatePersonContacts(person: Person) {
    val personContacts = ContactEntity.fromList(person.contacts)
    personContacts.forEach { personContactEntity -> personContactEntity.person = this }
    this.contacts.addAll(personContacts)
  }

  private fun updatePersonReferences(person: Person) {
    val personReferences = ReferenceEntity.fromList(person.references)
    personReferences.forEach { personReferenceEntity -> personReferenceEntity.person = this }
    this.references.addAll(personReferences)
  }

  private fun updatePersonSentences(person: Person) {
    val personSentences = SentenceInfoEntity.fromList(person.sentences)
    personSentences.forEach { personSentenceInfoEntity -> personSentenceInfoEntity.person = this }
    this.sentenceInfo.addAll(personSentences)
  }

  companion object {

    val empty = null

    fun PersonEntity?.extractSourceSystemId(): String? {
      return when (this?.sourceSystem) {
        DELIUS -> this.crn
        NOMIS -> this.prisonNumber
        COMMON_PLATFORM -> this.defendantId
        LIBRA -> this.defendantId
        else -> null
      }
    }

    fun List<ReferenceEntity>.getType(type: IdentifierType): List<ReferenceEntity> {
      return this.filter { it.identifierType == type }
    }

    fun PersonEntity?.shouldCreateOrUpdate(shouldCreate: () -> PersonEntity, shouldUpdate: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
      this == empty -> shouldCreate()
      else -> shouldUpdate(this)
    }

    fun from(person: Person): PersonEntity {
      val personEntity = PersonEntity(
        title = person.title,
        firstName = person.firstName,
        middleNames = person.middleNames?.joinToString(" ") { it },
        lastName = person.lastName,
        dateOfBirth = person.dateOfBirth,
        defendantId = person.defendantId,
        crn = person.crn,
        prisonNumber = person.prisonNumber,
        masterDefendantId = person.masterDefendantId,
        sourceSystem = person.sourceSystemType,
        ethnicity = person.ethnicity,
        nationality = person.nationality,
        religion = person.religion,
        currentlyManaged = person.currentlyManaged,
      )
      personEntity.updateChildEntities(person)
      return personEntity
    }
  }
}
