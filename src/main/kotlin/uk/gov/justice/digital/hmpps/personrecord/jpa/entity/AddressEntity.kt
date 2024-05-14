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
@Table(name = "address")
class AddressEntity(

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
    fun from(address: PersonAddress): AddressEntity? {
      if (address.postcode.isNullOrEmpty()) {
        return null
      }
      return AddressEntity(postcode = address.postcode)
    }

    fun fromList(personAddresses: List<PersonAddress>): List<AddressEntity> {
      return personAddresses.mapNotNull { from(it) }
    }
  }
}
