package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ContactDetails
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.PrisonerDetails
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
      return if (isContactDetailsPresent(person.homePhone, person.workPhone, person.mobile, person.primaryEmail)) {
        ContactEntity(
          homePhone = person.homePhone,
          workPhone = person.workPhone,
          mobile = person.mobile,
          primaryEmail = person.primaryEmail,
        )
      } else {
        null
      }
    }

    fun from(contactDetails: ContactDetails): ContactEntity? {
      return if (isContactDetailsPresent(contactDetails.getHomePhone(), null, contactDetails.getMobilePhone(), contactDetails.getEmail())) {
        ContactEntity(
          homePhone = contactDetails.getHomePhone(),
          mobile = contactDetails.getMobilePhone(),
          primaryEmail = contactDetails.getEmail(),
        )
      } else {
        null
      }
    }

    fun from(prisonerDetails: PrisonerDetails): ContactEntity? {
      return if (isContactDetailsPresent(prisonerDetails.getHomeAddress()?.getHomePhone(), null, prisonerDetails.getHomeAddress()?.getMobilePhone(), null)) {
        ContactEntity(
          homePhone = prisonerDetails.getHomeAddress()?.getHomePhone(),
          mobile = prisonerDetails.getHomeAddress()?.getMobilePhone(),
        )
      } else {
        null
      }
    }
    private fun isContactDetailsPresent(homePhone: String?, workPhone: String?, mobile: String?, primaryEmail: String?): Boolean {
      return sequenceOf(homePhone, workPhone, mobile, primaryEmail)
        .filterNotNull().any { it.isNotBlank() }
    }
  }
}
