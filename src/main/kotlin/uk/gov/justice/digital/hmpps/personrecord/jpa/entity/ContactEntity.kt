package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.Contact
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

@Entity
@Table(name = "contact")
class ContactEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = true)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = true,
  )
  var person: PersonEntity? = null,

  @ManyToOne(optional = true)
  @JoinColumn(
    name = "fk_address_id",
    referencedColumnName = "id",
    nullable = true,
  )
  var address: AddressEntity? = null,

  @Column(name = "contact_type")
  @Enumerated(EnumType.STRING)
  val contactType: ContactType,

  @Column(name = "contact_value")
  val contactValue: String? = null,

  @Column(name = "extension")
  val extension: String? = null,

  @Version
  var version: Int = 0,

) {
  companion object {

    fun from(contact: Contact): ContactEntity = ContactEntity(contactType = contact.contactType, contactValue = contact.contactValue, extension = contact.extension)
  }
}
