package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner.AllConvictedOffences
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import java.time.LocalDate

class SentenceInfo(
  val sentenceDate: LocalDate? = null,
  val primarySentence: Boolean? = true,
) {
  companion object {

    fun from(sentences: AllConvictedOffences): SentenceInfo {
      return SentenceInfo(
        sentenceDate = sentences.sentenceStartDate,
        primarySentence = sentences.primarySentence,
      )
    }

    fun from(sentences: Sentences): SentenceInfo {
      return SentenceInfo(
        sentenceDate = sentences.sentenceDate,
      )
    }

    fun from(sentenceInfoEntity: SentenceInfoEntity): SentenceInfo {
      return SentenceInfo(
        sentenceDate = sentenceInfoEntity.sentenceDate,
      )
    }
  }
}
