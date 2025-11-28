package uk.gov.justice.digital.hmpps.personrecord.eventlog

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseAlias
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Sentences
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.eventlog.RecordEventLog
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.EventLogService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomCro
import uk.gov.justice.digital.hmpps.personrecord.test.randomLongPnc
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import java.time.LocalDate

class EventLogServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var eventLogService: EventLogService

  @Test
  fun `should map exclude override to event log`() {
    val toRecord = createPersonWithNewKey(createRandomProbationPersonDetails())
    val fromRecord = createPersonWithNewKey(createRandomProbationPersonDetails())

    stubPersonMatchUpsert()
    excludeRecord(toRecord, fromRecord)

    val updatedToRecord = personRepository.findByMatchId(toRecord.matchId)!!

    val eventLog = eventLogService.logEvent(RecordEventLog(CPRLogEvents.CPR_RECORD_CREATED, updatedToRecord))

    assertThat(eventLog.overrideMarker).isNotNull()
    assertThat(eventLog.overrideScopes).isNotEmpty()
  }

  @Test
  fun `should dedupe and sort lists`() {
    val personEntity = createPersonWithNewKey(
      Person.from(
        ProbationCase(
          name = ProbationCaseName(firstName = randomName(), lastName = randomName()),
          identifiers = Identifiers(crn = randomCrn(), cro = randomCro(), pnc = randomLongPnc()),
          addresses = listOf(ProbationAddress(postcode = "ZX1 1AB"), ProbationAddress(postcode = "AB1 2BC"), ProbationAddress(postcode = "ZX1 1AB")),
          aliases = listOf(
            ProbationCaseAlias(
              name = ProbationCaseName(firstName = "Bob", lastName = "Smythe"),
              dateOfBirth = LocalDate.of(1970, 1, 1),
            ),
            ProbationCaseAlias(
              name = ProbationCaseName(firstName = "Bob", lastName = "Smith"),
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
