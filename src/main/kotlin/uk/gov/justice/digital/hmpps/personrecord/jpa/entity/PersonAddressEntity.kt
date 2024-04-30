package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.PersonAddress

@Entity
@Table(name = "person_address")
class PersonAddressEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
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
    fun from(address: PersonAddress): PersonAddressEntity? {
      if (!address.postcode.isNullOrEmpty()) {
        return PersonAddressEntity(
          postcode = address.postcode,
        )
      }
      return null
    }

    fun fromList(personAddresses: List<PersonAddress>): List<PersonAddressEntity> {
      return personAddresses.mapNotNull { from(it) }
    }

    private fun isAddressDetailsPresent(postcode: String?): Boolean {
      return sequenceOf(postcode)
        .filterNotNull().any { it.isNotBlank() }
    }
  }
}
