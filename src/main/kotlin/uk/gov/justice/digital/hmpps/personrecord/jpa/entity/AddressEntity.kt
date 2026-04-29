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
  val usages: MutableList<AddressUsageEntity> = mutableListOf(),

  @Column(name = "start_date")
  var startDate: LocalDate? = null,

  @Column(name = "end_date")
  var endDate: LocalDate? = null,

  @Column(name = "no_fixed_abode")
  val noFixedAbode: Boolean? = null,

  @Column(name = "address_full")
  val fullAddress: String? = null,

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
  val comment: String? = null,

  @Column(name = "uprn")
  var uprn: String? = null,

  @Enumerated(STRING)
  @Column(name = "record_type")
  var recordType: AddressRecordType? = null,

  @Enumerated(STRING)
  @Column(name = "status_code")
  val statusCode: AddressStatusCode? = null,

  @Version
  var version: Int = 0,
) {

  companion object {
    fun from(address: Address): AddressEntity = AddressEntity(
      startDate = address.startDate,
      endDate = address.endDate,
      noFixedAbode = address.noFixedAbode,
      postcode = address.postcode,
      fullAddress = address.fullAddress,
      subBuildingName = address.subBuildingName,
      buildingName = address.buildingName,
      buildingNumber = address.buildingNumber,
      thoroughfareName = address.thoroughfareName,
      dependentLocality = address.dependentLocality,
      postTown = address.postTown,
      county = address.county,
      countryCode = address.countryCode,
      uprn = address.uprn,
      recordType = address.recordType,
      comment = address.comment,
      statusCode = address.statusCode,
      usages = address.usages.map { AddressUsageEntity.from(it) }.toMutableList(),
      contacts = address.contacts.map { ContactEntity.from(it) }.toMutableList(),
    )
  }
}
