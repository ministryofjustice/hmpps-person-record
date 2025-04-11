package uk.gov.justice.digital.hmpps.personrecord.eventlog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Address
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.EventLogEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.EventLogRepository
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode

class EventLogIntTest : IntegrationTestBase() {

  @Autowired
  private lateinit var eventLogRepository: EventLogRepository

  @Test
  fun `should map person to event log`() {
    val personEntity = createPersonWithNewKey(
      Person.from(
        ProbationCase(
          name = Name(firstName = randomName(), middleNames = randomName(), lastName = randomName()),
          dateOfBirth = randomDate(),
          identifiers = Identifiers(crn = randomCrn(), cro = CROIdentifier.from(randomCro()), pnc = PNCIdentifier.from(randomPnc())),
          addresses = listOf(Address(postcode = randomPostcode())),
          aliases = listOf(
            ProbationCaseAlias(
              name = Name(firstName = randomName(), lastName = randomName()),
              dateOfBirth = randomDate(),
            ),
          ),
          sentences = listOf(Sentences(sentenceDate = randomDate()))
        ),
      ),
    )

    val eventLogEntity = EventLogEntity.from(personEntity)
    val eventLog = eventLogRepository.save(eventLogEntity)

    assertThat(eventLog).isNotNull()
    assertThat(eventLog.sourceSystemId).isEqualTo(personEntity.crn)
    assertThat(eventLog.sourceSystem).isEqualTo(SourceSystemType.DELIUS)
    assertThat(eventLog.matchId).isEqualTo(personEntity.matchId)
    assertThat(eventLog.uuid).isEqualTo(personEntity.personKey?.personId)
    assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
    assertThat(eventLog.firstName).isEqualTo(personEntity.firstName)
    assertThat(eventLog.middleNames).isEqualTo(personEntity.middleNames)
    assertThat(eventLog.lastName).isEqualTo(personEntity.lastName)
    assertThat(eventLog.dateOfBirth).isEqualTo(personEntity.dateOfBirth)
    assertThat(eventLog.firstNameAliases.size).isEqualTo(1)
    assertThat(eventLog.firstNameAliases.first()).isEqualTo(personEntity.pseudonyms.first().firstName)
    assertThat(eventLog.lastNameAliases.size).isEqualTo(1)
    assertThat(eventLog.lastNameAliases.first()).isEqualTo(personEntity.pseudonyms.first().lastName)
    assertThat(eventLog.lastNameAliases.size).isEqualTo(1)
    assertThat(eventLog.dateOfBirthAliases.first()).isEqualTo(personEntity.pseudonyms.first().dateOfBirth)
    assertThat(eventLog.postcodes.size).isEqualTo(1)
    assertThat(eventLog.postcodes.first()).isEqualTo(personEntity.addresses.first().postcode)
    assertThat(eventLog.cros.size).isEqualTo(1)
    assertThat(eventLog.cros.first()).isEqualTo(personEntity.references.getType(IdentifierType.CRO).first().identifierValue)
    assertThat(eventLog.pncs.size).isEqualTo(1)
    assertThat(eventLog.pncs.first()).isEqualTo(personEntity.references.getType(IdentifierType.PNC).first().identifierValue)
    assertThat(eventLog.sentenceDates.size).isEqualTo(1)
    assertThat(eventLog.sentenceDates.first()).isEqualTo(personEntity.sentenceInfo.first().sentenceDate)
    assertThat(eventLog.recordMergedTo).isNull()
    assertThat(eventLog.clusterComposition).isNull()
    assertThat(eventLog.eventTimestamp).isNull()
  }

  @Test
  fun `should map a merged person to event log`() {
    val mergedIntoPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
    var mergedToPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

    mergedToPerson = mergeRecord(mergedToPerson, mergedIntoPerson)

    val eventLogEntity = EventLogEntity.from(mergedToPerson)
    val eventLog = eventLogRepository.save(eventLogEntity)

    assertThat(eventLog).isNotNull()
    assertThat(mergedToPerson.mergedTo).isEqualTo(mergedIntoPerson.id)
  }
}
