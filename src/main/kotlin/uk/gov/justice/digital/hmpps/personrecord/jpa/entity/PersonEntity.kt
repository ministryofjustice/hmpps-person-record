package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.util.*

@Entity
@Table(name = "person")
@Audited
class PersonEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_id")
  val personId: UUID? = null,

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var deliusOffenders: MutableList<DeliusOffenderEntity> = mutableListOf(),

  @OneToMany(mappedBy = "person", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
  var hmctsDefendants: MutableList<HmctsDefendantEntity> = mutableListOf(),

) : BaseAuditedEntity() {
  companion object {
    fun new(): PersonEntity {
      val personEntity = PersonEntity(
        personId = UUID.randomUUID(),
      )

      personEntity.createdBy = PERSON_RECORD_SERVICE
      personEntity.lastUpdatedBy = PERSON_RECORD_SERVICE
      return personEntity
    }
  }
}
