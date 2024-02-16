package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Entity
@Table(name = "contact")
@Audited
class ContactEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "home_phone")
  val homePhone: String? = null,

  @Column(name = "work_phone")
  val workPhone: String? = null,

  @Column(name = "mobile")
  val mobile: String? = null,

  @Column(name = "primary_email")
  val primaryEmail: String? = null,

) : BaseAuditedEntity() {
  companion object {
    fun from(person: Person): ContactEntity {
      val addressEntity = ContactEntity(
        homePhone = person.homePhone,
        workPhone = person.workPhone,
        mobile = person.mobile,
        primaryEmail = person.primaryEmail,
      )
      addressEntity.createdBy = PERSON_RECORD_SERVICE
      addressEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
      return addressEntity
    }
  }

  fun isContactDetailsPresent(): Boolean {
    return homePhone?.isNotBlank() == true || workPhone?.isNotBlank() == true || mobile?.isNotBlank() == true
  }
}
