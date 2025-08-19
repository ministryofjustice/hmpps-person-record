package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.CreateUpdateService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Name as OffenderName

class CreateUpdateServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var createUpdateService: CreateUpdateService

  @Test
  fun `should not log record update when no change in matching fields`() {
    val person = createRandomProbationPersonDetails()
    val existingPerson = createPersonWithNewKey(person)

    createUpdateService.processPerson(person) { existingPerson }

    checkEventLogExist(existingPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED, times = 0)
  }

  @Test
  fun `should log record update when change in matching fields`() {
    val cId = randomCId()
    val firstName = randomName()
    val lastName = randomName()
    val person = Person.from(LibraHearingEvent(name = Name(firstName = firstName, lastName = lastName), cId = cId))
    val existingPerson = createPersonWithNewKey(person)

    val updatedPerson = Person.from(LibraHearingEvent(name = Name(firstName = randomName(), lastName = lastName), cId = cId))
    stubPersonMatchUpsert()
    stubPersonMatchScores()
    createUpdateService.processPerson(updatedPerson) { existingPerson }

    checkEventLogExist(existingPerson.cId!!, CPRLogEvents.CPR_RECORD_UPDATED)
  }

  @Test
  fun `should not log record update when no change in matching fields but different order`() {
    val firstName = randomName()
    val lastName = randomName()
    val crn = randomCrn()
    val postcode1 = "AB1 1AB"
    val postcode2 = "ZX1 2YW"
    val person = Person.from(
      ProbationCase(
        name = OffenderName(firstName = firstName, lastName = lastName),
        identifiers = Identifiers(crn = crn),
        addresses = listOf(ProbationAddress(postcode = postcode1), ProbationAddress(postcode = postcode2)),
      ),
    )
    val existingPerson = createPersonWithNewKey(person)

    val updatedPerson = Person.from(
      ProbationCase(
        name = OffenderName(firstName = firstName, lastName = lastName),
        identifiers = Identifiers(crn = crn),
        addresses = listOf(ProbationAddress(postcode = postcode2), ProbationAddress(postcode = postcode1)),
      ),
    )

    createUpdateService.processPerson(updatedPerson) { existingPerson }

    checkEventLogExist(existingPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED, times = 0)
  }
}
