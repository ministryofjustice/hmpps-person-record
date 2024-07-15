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

  @Column
  val postcode: String? = null,

  @Version
  var version: Int = 0,
) {
  companion object {
    fun from(address: Address): AddressEntity? {
      if (address.postcode.isNullOrEmpty()) {
        return null
      }
      return AddressEntity(postcode = address.postcode)
    }

    fun fromList(addresses: List<Address>): List<AddressEntity> {
      return addresses.mapNotNull { from(it) }
    }
  }
}
