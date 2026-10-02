package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonPseudonymSuffix

@Entity
@Table(name = "prison_pseudonym")
class PrisonPseudonymEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @OneToOne(optional = false)
  @JoinColumn(
    name = "fk_pseudonym_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var pseudonym: PseudonymEntity? = null,

  @Column(name = "suffix")
  @Enumerated(STRING)
  var suffix: PrisonPseudonymSuffix? = null,

  @Column(name = "alias_name_type")
  @Enumerated(STRING)
  var nameType: PrisonAliasNameType? = null,

  @Version
  var version: Int = 0,
)
