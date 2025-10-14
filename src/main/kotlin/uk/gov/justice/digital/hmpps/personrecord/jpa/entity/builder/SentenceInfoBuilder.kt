package uk.gov.justice.digital.hmpps.personrecord.jpa.entity.builder

import uk.gov.justice.digital.hmpps.personrecord.extensions.existsIn
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.SentenceInfo

object SentenceInfoBuilder {

  fun buildSentenceInfo(person: Person, personEntity: PersonEntity): List<SentenceInfoEntity> = person.sentences
    .mapNotNull { sentence ->
      sentence.existsIn(
        childEntities = personEntity.sentenceInfo,
        match = { ref, entity -> ref.matches(entity) },
        yes = { it },
        no = { SentenceInfoEntity.from(sentence) },
      )
    }.distinctBy { it.sentenceDate }

  private fun SentenceInfo.matches(entity: SentenceInfoEntity) = this.sentenceDate == entity.sentenceDate
}
