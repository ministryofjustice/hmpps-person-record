package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Entity
@Table(name = "address")
class AddressEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "address_line_one")
  val addressLineOne: String? = null,

  @Column(name = "address_line_two")
  val addressLineTwo: String? = null,

  @Column(name = "address_line_three")
  val addressLineThree: String? = null,

  @Column(name = "address_line_four")
  val addressLineFour: String? = null,

  @Column(name = "address_line_five")
  val addressLineFive: String? = null,

  @Column(name = "postcode")
  val postcode: String? = null,
  @Version
  var version: Int = 0,
) {
  companion object {
    fun from(person: Person): AddressEntity? {
      AddressEntity(
        addressLineOne = person.addressLineOne,
        addressLineTwo = person.addressLineTwo,
        addressLineThree = person.addressLineThree,
        addressLineFour = person.addressLineFour,
        addressLineFive = person.addressLineFive,
        postcode = person.postcode,
      ).apply {
        return if (isAddressDetailsPresent()) this else null
      }
    }
  }

  fun isAddressDetailsPresent(): Boolean {
    return sequenceOf(addressLineOne, addressLineTwo, addressLineThree, addressLineFour, addressLineFive, postcode)
      .filterNotNull().any { it.isNotBlank() }
  }
}
