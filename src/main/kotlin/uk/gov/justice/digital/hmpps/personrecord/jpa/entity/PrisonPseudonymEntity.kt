package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType.STRING
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonAliasNameType
import uk.gov.justice.digital.hmpps.personrecord.model.types.PrisonPseudonymSuffix

@Entity
@Table(name = "prison_pseudonym")
class PrisonPseudonymEntity(

  @Id
  var id: Long? = null,

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
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
