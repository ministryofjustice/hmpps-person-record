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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.OverrideMarkerType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.COMMON_PLATFORM
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.LIBRA
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "person")
class PersonEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

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
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var nationalities: MutableList<NationalityEntity> = mutableListOf(),

  @Column
  var religion: String? = null,

  @Column(name = "sexual_orientation")
  val sexualOrientation: String? = null,

  @Column(name = "sex_code")
  @Enumerated(STRING)
  var sexCode: SexCode? = null,

  @ManyToOne
  @JoinColumn(
    name = "fk_ethnicity_code_id",
    referencedColumnName = "id",
  )
  var ethnicityCode: EthnicityCodeEntity? = null,

  @Column
  var ethnicity: String? = null,

  @Column(name = "merged_to")
  var mergedTo: Long? = null,

  @Column
  @Enumerated(STRING)
  val sourceSystem: SourceSystemType,

  @Column(name = "match_id")
  val matchId: UUID,

  @Column(name = "c_id")
  var cId: String? = null,

  @Column(name = "last_modified")
  var lastModified: LocalDateTime? = null,

  @Version
  var version: Int = 0,

) {

  fun getAliases(): List<PseudonymEntity> = this.pseudonyms.filter { it.nameType.equals(NameType.ALIAS) }.sortedBy { it.id }
  fun getPrimaryName(): PseudonymEntity = this.pseudonyms.firstOrNull { it.nameType.equals(NameType.PRIMARY) } ?: PseudonymEntity(nameType = NameType.PRIMARY)

  fun getExcludeOverrideMarkers() = this.overrideMarkers.filter { it.markerType == OverrideMarkerType.EXCLUDE }

  fun getIncludeOverrideMarkers() = this.overrideMarkers.filter { it.markerType == OverrideMarkerType.INCLUDE }

  fun addExcludeOverrideMarker(excludeRecord: PersonEntity) {
    this.overrideMarkers.add(
      OverrideMarkerEntity(markerType = OverrideMarkerType.EXCLUDE, markerValue = excludeRecord.id, person = this),
    )
  }

  fun mergeTo(personEntity: PersonEntity) {
    this.mergedTo = personEntity.id
  }

  fun removeMergedLink() {
    this.mergedTo = null
  }

  fun isNotMerged() = this.mergedTo == null

  fun removePersonKeyLink() {
    this.personKey?.personEntities?.remove(this)
    this.personKey = null
  }

  fun update(person: Person) {
    this.defendantId = person.defendantId
    this.crn = person.crn
    this.prisonNumber = person.prisonNumber
    this.masterDefendantId = person.masterDefendantId
    this.nationality = person.nationality
    this.religion = person.religion
    this.cId = person.cId
    this.sexCode = person.sexCode
    this.lastModified = LocalDateTime.now()
    addresses.clear()
    contacts.clear()
    references.clear()
    sentenceInfo.clear()
    updateChildEntities(person)
  }

  private fun updateChildEntities(person: Person) {
    updatePersonAddresses(person)
    updatePersonContacts(person)
    updatePersonReferences(person)
    updatePersonSentences(person)
  }

  private fun updatePersonAddresses(person: Person) {
    val personAddresses = AddressEntity.fromList(person.addresses)
    personAddresses.forEach { personAddressEntity -> personAddressEntity.person = this }
    this.addresses.addAll(personAddresses)
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
    val personSentences = SentenceInfoEntity.fromList(person.sentences).distinctBy { it.sentenceDate }
    personSentences.forEach { personSentenceInfoEntity -> personSentenceInfoEntity.person = this }
    this.sentenceInfo.addAll(personSentences)
  }

  companion object {

    val empty = null

    fun PersonEntity?.extractSourceSystemId(): String? = when (this?.sourceSystem) {
      DELIUS -> this.crn
      NOMIS -> this.prisonNumber
      COMMON_PLATFORM -> this.defendantId
      LIBRA -> this.cId
      else -> null
    }

    fun List<ReferenceEntity>.getType(type: IdentifierType): List<ReferenceEntity> = this.filter { it.identifierType == type }

    fun PersonEntity?.exists(no: () -> PersonEntity, yes: (personEntity: PersonEntity) -> PersonEntity): PersonEntity = when {
      this == empty -> no()
      else -> yes(this)
    }

    fun new(person: Person): PersonEntity {
      val personEntity = PersonEntity(
        defendantId = person.defendantId,
        crn = person.crn,
        prisonNumber = person.prisonNumber,
        masterDefendantId = person.masterDefendantId,
        sourceSystem = person.sourceSystem,
        ethnicity = person.ethnicity,
        nationality = person.nationality,
        religion = person.religion,
        matchId = UUID.randomUUID(),
        cId = person.cId,
        lastModified = LocalDateTime.now(),
        sexCode = person.sexCode,
      )
      personEntity.updateChildEntities(person)
      return personEntity
    }
  }
}
