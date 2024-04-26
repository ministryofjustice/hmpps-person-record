package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Address
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

  @Column(name = "address_line_six")
  val addressLineSix: String? = null,

  @Column(name = "postcode")
  val postcode: String? = null,

  @Column(name = "address_line_eight")
  val addressLineEight: String? = null,

  @Version
  var version: Int = 0,
) {
  companion object {
    fun from(person: Person): AddressEntity? {
      return if (isAddressDetailsPresent(person.addressLineOne, person.addressLineTwo, person.addressLineThree, person.addressLineFour, person.addressLineFive, person.postcode)) {
        AddressEntity(
          addressLineOne = person.addressLineOne,
          addressLineTwo = person.addressLineTwo,
          addressLineThree = person.addressLineThree,
          addressLineFour = person.addressLineFour,
          addressLineFive = person.addressLineFive,
          addressLineSix = person.addressLineSix,
          postcode = person.postcode,
          addressLineEight = person.addressLineEight
        )
      } else {
        null
      }
    }
    fun from(person: Address): AddressEntity? {
      return if (isAddressDetailsPresent(null, null, null, null, null, person.postCode)) {
        AddressEntity(
          postcode = person.postCode,
        )
      } else {
        null
      }
    }
    private fun isAddressDetailsPresent(addressLineOne: String?, addressLineTwo: String?, addressLineThree: String?, addressLineFour: String?, addressLineFive: String?, postcode: String?): Boolean {
      return sequenceOf(addressLineOne, addressLineTwo, addressLineThree, addressLineFour, addressLineFive, postcode)
        .filterNotNull().any { it.isNotBlank() }
    }
  }
}
