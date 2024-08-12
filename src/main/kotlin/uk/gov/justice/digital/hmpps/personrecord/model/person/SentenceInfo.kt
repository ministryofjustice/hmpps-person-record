package uk.gov.justice.digital.hmpps.personrecord.model.person

import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import java.time.LocalDate

class SentenceInfo(
  val sentenceDate: LocalDate? = null,
) {
  companion object {
    fun from(sentenceInfo: Sentences): SentenceInfo {
      return SentenceInfo(
        sentenceDate = sentenceInfo.sentenceDate,
      )
    }

    fun fromList(sentenceDate: LocalDate?): SentenceInfo {
      return SentenceInfo(sentenceDate = sentenceDate)
    }
  }
}
