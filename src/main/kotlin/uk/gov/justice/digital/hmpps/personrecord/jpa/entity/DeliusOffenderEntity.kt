package uk.gov.justice.digital.hmpps.personrecord.jpa.entity


import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
@Entity
@Table(name = "delius_offender")
@Audited
class DeliusOffenderEntity(

  @Id
  @Column(name = "id")
  val id: Long,

  @Column(name = "crn")
  val crn: String,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "FK_PERSON_ID",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

) : BaseAuditedEntity()
