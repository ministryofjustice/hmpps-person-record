package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.ManyToMany
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.reference.EthnicityCodeEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.ALIAS
import uk.gov.justice.digital.hmpps.personrecord.model.types.NameType.PRIMARY
import uk.gov.justice.digital.hmpps.personrecord.model.types.SexualOrientation
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

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "fk_person_key_id", referencedColumnName = "id", nullable = true)
  var personKey: PersonKeyEntity? = null,

  @Column
  @OneToMany(mappedBy = "person", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var pseudonyms: MutableList<PseudonymEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var addresses: MutableList<AddressEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var contacts: MutableList<ContactEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var references: MutableList<ReferenceEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var sentenceInfo: MutableList<SentenceInfoEntity> = mutableListOf(),

  @Column(name = "override_marker")
  var overrideMarker: UUID? = null,

  @ManyToMany(fetch = EAGER)
  @JoinTable(
    name = "person_override_scope",
    joinColumns = [JoinColumn(name = "person_id")],
    inverseJoinColumns = [JoinColumn(name = "override_scope_id")],
  )
  val overrideScopes: MutableList<OverrideScopeEntity> = mutableListOf(),

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

  @Column(name = "birth_country_code")
  val birthCountryCode: String? = null,

  @Column
  @OneToMany(mappedBy = "person", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var nationalities: MutableList<NationalityEntity> = mutableListOf(),

  @Column
  var religion: String? = null,

  @Column(name = "sexual_orientation")
  @Enumerated(STRING)
  var sexualOrientation: SexualOrientation? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(
    name = "fk_ethnicity_code_id",
    referencedColumnName = "id",
  )
  var ethnicityCode: EthnicityCodeEntity? = null,

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

  fun getAliases(): List<PseudonymEntity> = this.pseudonyms.filter { it.nameType == ALIAS }.sortedBy { it.id }

  fun getPrimaryName(): PseudonymEntity = this.pseudonyms.firstOrNull { it.nameType == PRIMARY } ?: PseudonymEntity(nameType = PRIMARY)

  fun extractSourceSystemId(): String? = mapOf(
    DELIUS to this.crn,
    NOMIS to this.prisonNumber,
    COMMON_PLATFORM to this.defendantId,
    LIBRA to this.cId,
  )[this.sourceSystem]

  fun addOverrideMarker(scope: OverrideScopeEntity, marker: UUID = OverrideScopeEntity.newMarker()) {
    this.overrideMarker = this.overrideMarker ?: marker
    this.overrideScopes.add(scope)
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

  fun assignToPersonKey(personKey: PersonKeyEntity) {
    this.personKey = personKey
    personKey.personEntities.add(this)
  }

  fun update(person: Person) {
    this.defendantId = person.defendantId
    this.crn = person.crn
    this.prisonNumber = person.prisonNumber
    this.masterDefendantId = person.masterDefendantId
    this.religion = person.religion
    this.cId = person.cId
    this.sexualOrientation = person.sexualOrientation
    this.lastModified = LocalDateTime.now()
    updateChildEntities(person)
  }

  private fun updateChildEntities(person: Person) {
    updatePersonAddresses(person)
    updatePersonContacts(person)
    updatePersonSentences(person)
  }

  private fun updatePersonAddresses(person: Person) {
    this.addresses.clear()
    val personAddresses = AddressEntity.fromList(person.addresses)
    personAddresses.forEach { personAddressEntity -> personAddressEntity.person = this }
    this.addresses.addAll(personAddresses)
  }

  private fun updatePersonContacts(person: Person) {
    this.contacts.clear()
    val personContacts = ContactEntity.fromList(person.contacts)
    personContacts.forEach { personContactEntity -> personContactEntity.person = this }
    this.contacts.addAll(personContacts)
  }

  private fun updatePersonSentences(person: Person) {
    this.sentenceInfo.clear()
    val personSentences = SentenceInfoEntity.fromList(person.sentences).distinctBy { it.sentenceDate }
    personSentences.forEach { personSentenceInfoEntity -> personSentenceInfoEntity.person = this }
    this.sentenceInfo.addAll(personSentences)
  }

  companion object {

    val empty = null

    fun List<ContactEntity>.getType(type: ContactType): List<ContactEntity> = this.filter { it.contactType == type }

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
        religion = person.religion,
        matchId = UUID.randomUUID(),
        cId = person.cId,
        lastModified = LocalDateTime.now(),
        sexualOrientation = person.sexualOrientation,
      )
      personEntity.updateChildEntities(person)
      return personEntity
    }
  }
}
