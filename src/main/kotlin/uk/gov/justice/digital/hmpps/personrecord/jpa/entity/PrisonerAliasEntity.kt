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
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.Alias
import java.time.LocalDate

@Entity
@Table(name = "prisoner_alias")
class PrisonerAliasEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "fk_prisoner_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var prisoner: PrisonerEntity? = null,

  @Column(name = "first_name")
  val firstName: String? = null,

  @Column(name = "middle_name")
  val middleName: String? = null,

  @Column(name = "last_name")
  val lastName: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

  @Version
  var version: Int = 0,
) {

  companion object {
    private fun from(prisonerAlias: Alias): PrisonerAliasEntity? {
      val middleName = prisonerAlias.middleNames?.joinToString(" ") { it }
      return if (isAliasPresent(
          prisonerAlias.firstName,
          middleName,
          prisonerAlias.lastName,
          prisonerAlias.dob?.toString(),
        )
      ) {
        PrisonerAliasEntity(
          firstName = prisonerAlias.firstName,
          middleName = middleName,
          lastName = prisonerAlias.lastName,
          dateOfBirth = prisonerAlias.dob,
        )
      } else {
        null
      }
    }

    fun fromList(prisonerAliases: List<Alias>): List<PrisonerAliasEntity> {
      return prisonerAliases.mapNotNull { from(it) }
    }

    private fun isAliasPresent(firstName: String?, middleName: String?, surname: String?, dateOfBirth: String?): Boolean {
      return sequenceOf(firstName, middleName, surname, dateOfBirth)
        .filterNotNull().any { it.isNotBlank() }
    }
  }
}
