package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.CROIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.jpa.converter.PNCIdentifierConverter
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
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

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var aliases: MutableList<AliasEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var addresses: MutableList<AddressEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
  var contacts: MutableList<ContactEntity> = mutableListOf(),

  @Column
  @Convert(converter = PNCIdentifierConverter::class)
  var pnc: PNCIdentifier? = null,

  @Column
  var crn: String? = null,

  @Column(name = "prison_number")
  var prisonNumber: String? = null,

  @Column(name = "offender_id")
  val offenderId: String? = null,

  @Column(name = "defendant_id")
  var defendantId: String? = null,

  @Column(name = "master_defendant_id")
  var masterDefendantId: String? = null,

  @Column
  @Convert(converter = CROIdentifierConverter::class)
  var cro: CROIdentifier? = null,

  @Column
  var fingerprint: Boolean = false,

  @Column(name = "national_insurance_number")
  var nationalInsuranceNumber: String? = null,

  @Column(name = "driver_license_number")
  var driverLicenseNumber: String? = null,

  @Column(name = "arrest_summons_number")
  var arrestSummonsNumber: String? = null,

  @Column(name = "date_of_birth")
  var dateOfBirth: LocalDate? = null,

  @Column
  @Enumerated(STRING)
  val sourceSystem: SourceSystemType,

  @Version
  var version: Int = 0,

) {
  fun update(person: Person): PersonEntity {
    this.title = person.title
    this.firstName = person.firstName
    this.middleNames = person.middleNames?.joinToString(" ") { it }
    this.lastName = person.lastName
    this.dateOfBirth = person.dateOfBirth
    this.defendantId = person.defendantId
    this.pnc = person.otherIdentifiers?.pncIdentifier
    this.crn = person.otherIdentifiers?.crn
    this.cro = person.otherIdentifiers?.croIdentifier
    this.fingerprint = person.otherIdentifiers?.croIdentifier?.fingerprint ?: false
    this.prisonNumber = person.otherIdentifiers?.prisonNumber
    this.driverLicenseNumber = person.driverNumber
    this.arrestSummonsNumber = person.arrestSummonsNumber
    this.masterDefendantId = person.masterDefendantId
    this.nationalInsuranceNumber = person.nationalInsuranceNumber
    updateChildEntities(person)
    return this
  }

  private fun updateChildEntities(person: Person) {
    updatePersonAddresses(person)
    updatePersonContacts(person)
    updatePersonAliases(person)
  }

  private fun updatePersonAddresses(person: Person) {
    val personAddresses = AddressEntity.fromList(person.addresses)
    personAddresses.forEach { personAddressEntity -> personAddressEntity.person = this }
    this.addresses.addAll(personAddresses)
  }

  private fun updatePersonAliases(person: Person) {
    val personAliases = AliasEntity.fromList(person.aliases)
    personAliases.forEach { personAliasEntity -> personAliasEntity.person = this }
    this.aliases.addAll(personAliases)
  }

  private fun updatePersonContacts(person: Person) {
    val personContacts = ContactEntity.fromList(person.contacts)
    personContacts.forEach { personContactEntity -> personContactEntity.person = this }
    this.contacts.addAll(personContacts)
  }

  companion object {
    fun from(person: Person): PersonEntity {
      val personEntity = PersonEntity(
        title = person.title,
        firstName = person.firstName,
        middleNames = person.middleNames?.joinToString(" ") { it },
        lastName = person.lastName,
        dateOfBirth = person.dateOfBirth,
        defendantId = person.defendantId,
        pnc = person.otherIdentifiers?.pncIdentifier,
        crn = person.otherIdentifiers?.crn,
        cro = person.otherIdentifiers?.croIdentifier,
        fingerprint = person.otherIdentifiers?.croIdentifier?.fingerprint ?: false,
        prisonNumber = person.otherIdentifiers?.prisonNumber,
        driverLicenseNumber = person.driverNumber,
        arrestSummonsNumber = person.arrestSummonsNumber,
        masterDefendantId = person.masterDefendantId,
        nationalInsuranceNumber = person.nationalInsuranceNumber,
        sourceSystem = person.sourceSystemType,
      )
      personEntity.updateChildEntities(person)
      return personEntity
    }
  }
}
