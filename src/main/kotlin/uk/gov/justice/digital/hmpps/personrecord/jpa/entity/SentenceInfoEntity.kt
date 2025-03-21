package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.SentenceInfo
import java.time.LocalDate

@Entity
@Table(name = "sentence_info")
class SentenceInfoEntity(

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "sentence_date")
  var sentenceDate: LocalDate? = null,

  @Version
  var version: Int = 0,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_person_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var person: PersonEntity? = null,

) {
  companion object {
    fun from(sentenceInfo: SentenceInfo): SentenceInfoEntity? {
      val isPrimarySentencePresent = sentenceInfo.sentenceDate != null && sentenceInfo.primarySentence == true
      return when {
        isPrimarySentencePresent -> SentenceInfoEntity(sentenceDate = sentenceInfo.sentenceDate)
        else -> null
      }
    }

    fun fromList(sentenceInfo: List<SentenceInfo>): List<SentenceInfoEntity> = sentenceInfo.mapNotNull { from(it) }
  }
}
