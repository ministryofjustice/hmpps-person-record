package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.person.SentenceInfo
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate

@ActiveProfiles("e2e")
class E2ETestBase : MessagingTestBase() {

  @Autowired
  private lateinit var personMatchService: PersonMatchService

  override fun createPerson(person: Person, personKeyEntity: PersonKeyEntity?): PersonEntity {
    val personEntity = super.createPerson(person, personKeyEntity)
    personMatchService.saveToPersonMatch(personEntity)
    return personEntity
  }

  override fun excludeRecord(sourceRecord: PersonEntity, excludingRecord: PersonEntity) {
    super.excludeRecord(sourceRecord, excludingRecord)
    personMatchService.saveToPersonMatch(sourceRecord)
    personMatchService.saveToPersonMatch(excludingRecord)
  }

  internal fun createProbationPersonFrom(from: Person, crn: String = randomCrn()): Person = from.copy(crn = crn)

  /*
  Remove matching fields to reduce match weight below the join threshold but keep above fracture threshold
   */
  internal fun Person.aboveFracture(): Person = this.copy(
    references = this.references.filterNot { it.identifierType == IdentifierType.PNC || it.identifierType == IdentifierType.CRO },
    sentences = emptyList(),
  )

  internal fun Person.withChangedMatchDetails(): Person = this.copy(
    sentences = this.sentences + SentenceInfo(
      randomDate(),
    ),
  )
}
