package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.SentenceInfo

class SentenceInfoBuilder : ChildBuilder {

  override fun build(person: Person, personEntity: PersonEntity) {
    val builtSentences = personEntity.buildSentenceInfo(person).distinctBy { it.sentenceDate }
    personEntity.sentenceInfo.clear()
    builtSentences.forEach { personSentenceInfoEntity -> personSentenceInfoEntity.person = personEntity }
    personEntity.sentenceInfo.addAll(builtSentences)
  }

  private fun PersonEntity.buildSentenceInfo(person: Person): List<SentenceInfoEntity> = person.sentences
    .mapNotNull { sentence ->
      sentence.existsIn(
        childEntities = this.sentenceInfo,
        match = { ref, entity -> ref.matches(entity) },
        yes = { it },
        no = { SentenceInfoEntity.from(sentence) },
      )
    }

  private fun SentenceInfo.matches(entity: SentenceInfoEntity) = this.sentenceDate == entity.sentenceDate
}
