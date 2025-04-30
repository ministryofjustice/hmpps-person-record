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
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.getType
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.CROIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.EventLogService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomDate
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomPostcode
import java.time.LocalDate

class EventLogServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var eventLogService: EventLogService

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
          sentences = listOf(Sentences(sentenceDate = randomDate())),
        ),
      ),
    )

    val eventLog = eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_UPDATED, personEntity))

    assertThat(eventLog).isNotNull()
    assertThat(eventLog.sourceSystemId).isEqualTo(personEntity.crn)
    assertThat(eventLog.sourceSystem).isEqualTo(SourceSystemType.DELIUS)
    assertThat(eventLog.matchId).isEqualTo(personEntity.matchId)
    assertThat(eventLog.uuid).isEqualTo(personEntity.personKey?.personUUID)
    assertThat(eventLog.uuidStatusType).isEqualTo(UUIDStatusType.ACTIVE)
    assertThat(eventLog.firstName).isEqualTo(personEntity.getPrimaryName().firstName)
    assertThat(eventLog.middleNames).isEqualTo(personEntity.getPrimaryName().middleNames)
    assertThat(eventLog.lastName).isEqualTo(personEntity.getPrimaryName().lastName)
    assertThat(eventLog.dateOfBirth).isEqualTo(personEntity.getPrimaryName().dateOfBirth)
    assertThat(eventLog.firstNameAliases.size).isEqualTo(1)
    assertThat(eventLog.firstNameAliases.first()).isEqualTo(personEntity.getAliases().first().firstName)
    assertThat(eventLog.lastNameAliases.size).isEqualTo(1)
    assertThat(eventLog.lastNameAliases.first()).isEqualTo(personEntity.getAliases().first().lastName)
    assertThat(eventLog.lastNameAliases.size).isEqualTo(1)
    assertThat(eventLog.dateOfBirthAliases.first()).isEqualTo(personEntity.getAliases().first().dateOfBirth)
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
    assertThat(eventLog.eventTimestamp).isNotNull()
    assertThat(eventLog.eventType).isEqualTo(CPRLogEvents.CPR_RECORD_UPDATED)
    assertThat(eventLog.excludeOverrideMarkers.size).isEqualTo(0)
    assertThat(eventLog.includeOverrideMarkers.size).isEqualTo(0)
  }

  @Test
  fun `should map a merged person to event log`() {
    val mergedIntoPerson = createPersonWithNewKey(createRandomProbationPersonDetails())
    var mergedToPerson = createPersonWithNewKey(createRandomProbationPersonDetails())

    mergedToPerson = mergeRecord(mergedToPerson, mergedIntoPerson)

    val eventLog = eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_CREATED, mergedToPerson))

    assertThat(eventLog).isNotNull()
    assertThat(mergedToPerson.mergedTo).isEqualTo(mergedIntoPerson.id)
  }

  @Test
  fun `should map exclude override to event log`() {
    val toRecord = createPersonWithNewKey(createRandomProbationPersonDetails())
    val fromRecord = createPersonWithNewKey(createRandomProbationPersonDetails())

    excludeRecord(toRecord, fromRecord)

    val updatedToRecord = personRepository.findByMatchId(toRecord.matchId)!!

    val eventLog = eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_CREATED, updatedToRecord))

    assertThat(eventLog.excludeOverrideMarkers.size).isEqualTo(1)
    assertThat(eventLog.excludeOverrideMarkers.first()).isEqualTo(fromRecord.id)
  }

  @Test
  fun `should dedupe and sort lists`() {
    val personEntity = createPersonWithNewKey(
      Person.from(
        ProbationCase(
          name = Name(firstName = randomName(), lastName = randomName()),
          identifiers = Identifiers(crn = randomCrn(), cro = CROIdentifier.from(randomCro()), pnc = PNCIdentifier.from(randomPnc())),
          addresses = listOf(Address(postcode = "ZX1 1AB"), Address(postcode = "AB1 2BC"), Address(postcode = "ZX1 1AB")),
          aliases = listOf(
            ProbationCaseAlias(
              name = Name(firstName = "Bob", lastName = "Smythe"),
              dateOfBirth = LocalDate.of(1970, 1, 1),
            ),
            ProbationCaseAlias(
              name = Name(firstName = "Bob", lastName = "Smith"),
              dateOfBirth = LocalDate.of(1980, 1, 1),
            ),
          ),
          sentences = listOf(
            Sentences(sentenceDate = LocalDate.of(1980, 1, 1)),
            Sentences(sentenceDate = LocalDate.of(1990, 1, 1)),
            Sentences(sentenceDate = LocalDate.of(1980, 1, 1)),
          ),
        ),
      ),
    )

    val eventLog = eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_CREATED, personEntity))

    assertThat(eventLog.postcodes.size).isEqualTo(2)
    assertThat(eventLog.postcodes[0]).isEqualTo("AB1 2BC")
    assertThat(eventLog.postcodes[1]).isEqualTo("ZX1 1AB")

    assertThat(eventLog.firstNameAliases.size).isEqualTo(1)
    assertThat(eventLog.firstNameAliases[0]).isEqualTo("Bob")

    assertThat(eventLog.lastNameAliases.size).isEqualTo(2)
    assertThat(eventLog.lastNameAliases[0]).isEqualTo("Smith")
    assertThat(eventLog.lastNameAliases[1]).isEqualTo("Smythe")

    assertThat(eventLog.sentenceDates.size).isEqualTo(2)
    assertThat(eventLog.sentenceDates[0]).isEqualTo(LocalDate.of(1980, 1, 1))
    assertThat(eventLog.sentenceDates[1]).isEqualTo(LocalDate.of(1990, 1, 1))
  }
}
