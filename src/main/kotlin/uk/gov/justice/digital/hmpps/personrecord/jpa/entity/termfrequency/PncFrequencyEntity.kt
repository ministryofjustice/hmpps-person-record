package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.termfrequency

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.client.model.termfrequency.TermFrequency

@Entity
@Table(name = "pnc_frequency", schema = "personmatchscore")
class PncFrequencyEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  val pnc: String? = null,

  @Column
  val frequency: Double? = null,

) {
  companion object {
    fun from(termFrequency: TermFrequency): PncFrequencyEntity {
      return PncFrequencyEntity(pnc = termFrequency.getTerm(), frequency = termFrequency.getFrequency())
    }
  }
}
