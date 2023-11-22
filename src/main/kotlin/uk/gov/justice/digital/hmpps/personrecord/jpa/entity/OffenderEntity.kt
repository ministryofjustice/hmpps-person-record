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
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.model.Person

@Entity
@Table(name = "offender")
@Audited
class OffenderEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "crn")
  val crn: String,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

) : BaseAuditedEntity() {
  companion object {
    fun from(person: Person): OffenderEntity {
      return person.otherIdentifiers?.crn?.let {
        val offenderEntity = OffenderEntity(
          crn = it,
        )
        offenderEntity.createdBy = PERSON_RECORD_SERVICE
        offenderEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
        return offenderEntity
      } ?: throw java.lang.IllegalArgumentException("Missing CRN")
    }

    fun from(offenderDetail: OffenderDetail): OffenderEntity {
      val offenderEntity = OffenderEntity(
        crn = offenderDetail.otherIds.crn,
      )
      offenderEntity.createdBy = PERSON_RECORD_SERVICE
      offenderEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
      return offenderEntity
    }
  }
}
