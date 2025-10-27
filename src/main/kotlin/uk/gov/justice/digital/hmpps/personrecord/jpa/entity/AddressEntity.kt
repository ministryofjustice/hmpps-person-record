package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
import uk.gov.justice.digital.hmpps.personrecord.model.types.RecordType
import java.time.LocalDate

@Entity
@Table(name = "address")
class AddressEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "start_date")
  val startDate: LocalDate? = null,

  @Column(name = "end_date")
  val endDate: LocalDate? = null,

  @Column(name = "no_fixed_abode")
  val noFixedAbode: Boolean? = null,

  @Column(name = "address_full")
  val fullAddress: String? = null,

  @Column
  val postcode: String? = null,

  @Column(name = "address_type")
  val type: String? = null,

  @Column(name = "sub_building_name")
  val subBuildingName: String? = null,

  @Column(name = "building_name")
  val buildingName: String? = null,

  @Column(name = "building_number")
  val buildingNumber: String? = null,

  @Column(name = "thoroughfare_name")
  val thoroughfareName: String? = null,

  @Column(name = "dependent_locality")
  val dependentLocality: String? = null,

  @Column(name = "post_town")
  val postTown: String? = null,

  @Column(name = "county")
  val county: String? = null,

  @Column(name = "country_code")
  val countryCode: String? = null,

  @Column(name = "uprn")
  val uprn: String? = null,

  @Enumerated(STRING)
  @Column(name = "record_type")
  var recordType: RecordType? = null,

  @Version
  var version: Int = 0,
) {

  fun isPrimary() = this.recordType == RecordType.PRIMARY

  fun isPrevious() = this.recordType == RecordType.PREVIOUS

  companion object {
    fun from(address: Address, recordType: RecordType? = null): AddressEntity = AddressEntity(
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
      recordType = recordType,
    )

    fun toPrimary(address: Address): AddressEntity = from(address, RecordType.PRIMARY)

    fun toPrevious(address: Address): AddressEntity = from(address, RecordType.PREVIOUS)
  }
}
