package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType.ALL
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.EAGER
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Generated
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressRecordType
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import uk.gov.justice.digital.hmpps.personrecord.model.types.CountryCode
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "address")
class AddressEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(
    name = "update_id",
    insertable = false,
    updatable = false,
    nullable = false,
  )
  @Generated
  var updateId: UUID? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column
  @OneToMany(mappedBy = "address", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var contacts: MutableList<ContactEntity> = mutableListOf(),

  @Column
  @OneToMany(mappedBy = "address", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
  var usages: MutableList<AddressUsageEntity> = mutableListOf(),

  @Column(name = "start_date")
  var startDate: LocalDate? = null,

  @Column(name = "end_date")
  var endDate: LocalDate? = null,

  @Column(name = "no_fixed_abode")
  var noFixedAbode: Boolean? = null,

  @Column(name = "address_full")
  var fullAddress: String? = null,

  @Column
  var postcode: String? = null,

  @Column(name = "sub_building_name")
  var subBuildingName: String? = null,

  @Column(name = "building_name")
  var buildingName: String? = null,

  @Column(name = "building_number")
  var buildingNumber: String? = null,

  @Column(name = "thoroughfare_name")
  var thoroughfareName: String? = null,

  @Column(name = "dependent_locality")
  var dependentLocality: String? = null,

  @Column(name = "post_town")
  var postTown: String? = null,

  @Column(name = "county")
  var county: String? = null,

  @Column(name = "country_code")
  @Enumerated(STRING)
  var countryCode: CountryCode? = null,

  @Column(name = "comment")
  var comment: String? = null,

  @Column(name = "uprn")
  var uprn: String? = null,

  @Enumerated(STRING)
  @Column(name = "record_type")
  var recordType: AddressRecordType? = null,

  @Enumerated(STRING)
  @Column(name = "status_code")
  var statusCode: AddressStatusCode? = null,

  @Version
  var version: Int = 0,
) {

  fun update(address: Address) {
    this.noFixedAbode = address.noFixedAbode
    this.startDate = address.startDate
    this.endDate = address.endDate
    this.postcode = address.postcode
    this.fullAddress = address.fullAddress
    this.subBuildingName = address.subBuildingName
    this.buildingName = address.buildingName
    this.buildingNumber = address.buildingNumber
    this.thoroughfareName = address.thoroughfareName
    this.dependentLocality = address.dependentLocality
    this.postTown = address.postTown
    this.county = address.county
    this.countryCode = address.countryCode
    this.uprn = address.uprn
    this.comment = address.comment
    this.contacts = address.contacts.map { ContactEntity.from(it).also { ue -> ue.address = this } }.toMutableList()
    this.statusCode = address.statusCode
    this.usages = address.usages.map { AddressUsageEntity.from(it).also { ue -> ue.address = this } }.toMutableList()
    this.recordType = address.recordType
    // TODO: determine how best to update the child entities
  }

  companion object {
    fun from(address: Address): AddressEntity = AddressEntity().also { it.update(address) }
  }
}
