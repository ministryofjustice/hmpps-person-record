package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import java.time.LocalDate

@Entity
@Table(name = "hmcts_defendant")
@Audited
class HmctsDefendantEntity(

  @Id
  @Column(name = "id")
  val id: Long,

  @ManyToOne(optional = false, cascade = [CascadeType.ALL])
  @JoinColumn(
    name = "FK_PERSON_ID",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

  @Column(name = "defendant_id")
  val defendantId: String,

  @Column(name = "pnc_number")
  val pncNumber: String? = null,

  @Column(name = "crn")
  val crn: String? = null,

  @Column(name = "cro")
  val cro: String? = null,

  @Column(name = "title")
  val title: String? = null,

  @Column(name = "forename_one")
  val forenameOne: String? = null,

  @Column(name = "forename_two")
  val forenameTwo: String? = null,

  @Column(name = "forename_three")
  val forenameThree: String? = null,

  @Column(name = "surname")
  val surname: String? = null,

  @Column(name = "address_line_one")
  val addressLineOne: String? = null,

  @Column(name = "address_line_two")
  val addressLineTwo: String? = null,

  @Column(name = "address_line_three")
  val addressLineThree: String? = null,

  @Column(name = "address_line_four")
  val addressLineFour: String? = null,

  @Column(name = "address_line_five")
  val addressLineFive: String? = null,

  @Column(name = "postcode")
  val postcode: String? = null,

  @Column(name = "sex")
  val sex: String? = null,

  @Column(name = "nationality_one")
  val nationalityOne: String? = null,

  @Column(name = "nationality_two")
  val nationalityTwo: String? = null,

  @Column(name = "date_of_birth")
  val dateOfBirth: LocalDate? = null,

) : BaseAuditedEntity()
