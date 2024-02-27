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
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Aliases
import java.time.LocalDate

@Entity
@Table(name = "offender_alias")
class OffenderAliasEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_offender_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var offender: OffenderEntity? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_name")
  val middleName: String? = null,

  @Column(name = "surname")
  val surname: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Column(name = "alias_offender_id")
  val aliasOffenderId: String? = null,

  @Version
  var version: Int = 0,
) {

  companion object {
    private fun from(offenderAlias: Aliases): OffenderAliasEntity? {
      return if (isAliasPresent(
          offenderAlias.firstName,
          offenderAlias.middleNames?.joinToString { " " },
          offenderAlias.surName,
          offenderAlias.dataOfBirth?.toString(),
          offenderAlias.id,
        )
      ) {
        OffenderAliasEntity(
          firstName = offenderAlias.firstName,
          middleName = offenderAlias.middleNames?.joinToString { " " },
          surname = offenderAlias.surName,
          aliasOffenderId = offenderAlias.id,
        )
      } else {
        null
      }
    }

    fun fromList(offenderAliases: List<Aliases>): List<OffenderAliasEntity> {
      return offenderAliases.mapNotNull { from(it) }
    }

    private fun isAliasPresent(firstName: String?, middleName: String?, surname: String?, dateOfBirth: String?, id: String?): Boolean {
      return sequenceOf(firstName, middleName, surname, dateOfBirth, id)
        .filterNotNull().any { it.isNotBlank() }
    }
  }
}
