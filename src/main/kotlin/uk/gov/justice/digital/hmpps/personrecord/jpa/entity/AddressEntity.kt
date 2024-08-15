package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.Address
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

  @Version
  var version: Int = 0,
) {
  companion object {
    fun from(address: Address): AddressEntity? {
      if (address.postcode.isNullOrEmpty()) {
        return null
      }
      return AddressEntity(
        startDate = address.startDate,
        endDate = address.endDate,
        noFixedAbode = address.noFixedAbode,
        postcode = address.postcode,
        fullAddress = address.fullAddress,
      )
    }

    fun fromList(addresses: List<Address>): List<AddressEntity> {
      return addresses.mapNotNull { from(it) }
    }
  }
}
