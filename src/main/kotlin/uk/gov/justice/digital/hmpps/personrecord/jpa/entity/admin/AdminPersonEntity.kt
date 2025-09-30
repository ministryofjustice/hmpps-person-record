package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.admin

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType.LAZY
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType

@Entity
@Table(name = "person")
class AdminPersonEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Long? = null,

  @ManyToOne(fetch = LAZY)
  @JoinColumn(name = "fk_person_key_id", referencedColumnName = "id", nullable = true)
  var personKey: PersonKeyEntity? = null,

  @Column
  @Enumerated(STRING)
  val sourceSystem: SourceSystemType,

)
