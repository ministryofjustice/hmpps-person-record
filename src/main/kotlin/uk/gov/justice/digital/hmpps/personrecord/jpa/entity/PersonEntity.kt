package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import uk.gov.justice.digital.hmpps.personrecord.model.Person
import java.util.*

private const val PERSON_RECORD_SERVICE = "PERSON-RECORD-SERVICE"

@Entity
@Table(name = "person")
@Audited
class PersonEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @OneToMany(mappedBy = "person", fetch = FetchType.EAGER)
  val deliusOffenders: List<DeliusOffenderEntity>? = null,

  @OneToMany(mappedBy = "person", fetch = FetchType.EAGER)
  val hmctsDefendants: List<HmctsDefendantEntity>? = null,

) : BaseAuditedEntity() {
  companion object {
    fun from(person: Person): PersonEntity {
      //TODO map from Person
      person.dateOfBirth
      val personEntity = PersonEntity(
        personId = UUID.randomUUID(),
      )

      personEntity.createdBy = PERSON_RECORD_SERVICE
      personEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
      return personEntity
    }
  }
}
