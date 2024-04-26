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
import jakarta.persistence.Version

@Entity
@Table(name = "person_alias")
class PersonAliasEntity (

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity,

  @Column
  val firstName: String? = null,

  @Column
  val middleNames: String? = null,

  @Column
  val surname: String? = null,

  @Version
  var version: Int = 0,

)