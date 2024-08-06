package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.termfrequency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "pnc_frequency", schema = "personmatchscore")
class PncFrequencyEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  val pnc: String? = null,

  @Column
  val frequency: BigDecimal? = null,

)
