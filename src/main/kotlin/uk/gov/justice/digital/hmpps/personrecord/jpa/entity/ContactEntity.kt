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
@Table(name = "contact")
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

  @Version
  var version: Int = 0,

) {
  companion object {
    fun from(person: Person): ContactEntity? {
      ContactEntity(
        homePhone = person.homePhone,
        workPhone = person.workPhone,
        mobile = person.mobile,
        primaryEmail = person.primaryEmail,
      ).apply {
        return if (isContactDetailsPresent()) this else null
      }
    }
  }

  fun isContactDetailsPresent(): Boolean {
    return sequenceOf(homePhone, workPhone, mobile, primaryEmail)
      .filterNotNull().any { it.isNotBlank() }
  }
}
