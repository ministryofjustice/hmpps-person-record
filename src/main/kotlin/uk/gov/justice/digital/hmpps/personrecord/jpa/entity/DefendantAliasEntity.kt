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
import uk.gov.justice.digital.hmpps.personrecord.model.PersonAlias

@Entity
@Table(name = "defendant_alias")
class DefendantAliasEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_defendant_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var defendant: DefendantEntity? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_name")
  val middleName: String? = null,

  @Column(name = "surname")
  val surname: String? = null,

  @Version
  var version: Int = 0,
) {

  companion object {
    fun from(personAlias: PersonAlias): DefendantAliasEntity {
      val defendantAliasEntity = DefendantAliasEntity(
        firstName = personAlias.firstName,
        middleName = personAlias.middleName,
        surname = personAlias.lastName,
      )
      return defendantAliasEntity
    }
  }
}
