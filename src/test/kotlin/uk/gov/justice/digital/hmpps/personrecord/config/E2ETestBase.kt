package uk.gov.justice.digital.hmpps.personrecord.config

import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn

class E2ETestBase: MessagingTestBase() {

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

  internal fun createProbationPersonFrom(from: Person, crn: String = randomCrn()): Person = Person.from(
    ProbationCase(
      name = ProbationCaseName(firstName = from.firstName, middleNames = from.middleNames, lastName = from.lastName),
      identifiers = Identifiers(crn = crn, pnc = from.getPnc(), cro = from.getCro()),
      addresses = from.addresses.map { ProbationAddress(postcode = it.postcode) },
      aliases = from.aliases.map {
        ProbationCaseAlias(
          ProbationCaseName(it.firstName, it.lastName, it.middleNames),
          it.dateOfBirth,
        )
      },
      sentences = from.sentences.map { Sentences(it.sentenceDate) },
    ),
  )
}