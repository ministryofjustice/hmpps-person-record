package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
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
import uk.gov.justice.digital.hmpps.personrecord.model.PersonContact
import uk.gov.justice.digital.hmpps.personrecord.model.types.ContactType

@Entity
@Table(name = "person_contact")
class PersonContactEntity(

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

  @Column(name = "contact_type")
  @Enumerated(EnumType.STRING)
  val contactType: ContactType,

  @Column(name = "contact_value")
  val contactValue: String? = null,

  @Version
  var version: Int = 0,

) {
  companion object {

    private fun from(personContact: PersonContact): PersonContactEntity {
      return PersonContactEntity(contactType = personContact.contactType, contactValue = personContact.contactValue)
    }

    fun fromList(personContacts: List<PersonContact>): List<PersonContactEntity> {
      return personContacts.filterNot { it.contactValue.isNullOrEmpty() }.map { from(it) }
    }
  }
}
