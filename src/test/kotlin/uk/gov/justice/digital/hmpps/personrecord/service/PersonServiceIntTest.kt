package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.client.model.court.libra.Name
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.Identifiers
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationAddress
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCase
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.service.eventlog.CPRLogEvents
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import uk.gov.justice.digital.hmpps.personrecord.test.randomCId
import uk.gov.justice.digital.hmpps.personrecord.test.randomCrn
import uk.gov.justice.digital.hmpps.personrecord.test.randomName
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import uk.gov.justice.digital.hmpps.personrecord.client.model.offender.ProbationCaseName as OffenderName

class PersonServiceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var personService: PersonService

  @MockitoSpyBean
  private lateinit var mockReclusterService: ReclusterService

  @MockitoSpyBean
  private lateinit var mockPersonMatchService: PersonMatchService

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
    personService.processPerson(updatedPerson) { existingPerson }

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

    personService.processPerson(updatedPerson) { existingPerson }
    checkEventLog(existingPerson.crn!!, CPRLogEvents.CPR_RECORD_UPDATED) { logEvents ->
      assertThat(logEvents).isEmpty()
    }
  }

  @Test
  fun `should not save to person match or recluster passive records on update`() {
    val prisonNumber = randomPrisonNumber()
    val originalEntity = createPersonWithNewKey(createRandomPrisonPersonDetails(prisonNumber)) { markAsPassive() }
    val originalCluster = originalEntity.personKey

    val updatedPerson = Person.from(originalEntity).copy(firstName = randomName())

    personService.processPerson(updatedPerson) { originalEntity }

    awaitAssert {
      val updatedPerson = personRepository.findByPrisonNumber(prisonNumber)
      assertThat(updatedPerson!!.personKey!!.personUUID).isEqualTo(originalCluster!!.personUUID)
      verifyNoInteractions(mockReclusterService, mockPersonMatchService)
      checkEventLogExist(updatedPerson.prisonNumber!!, CPRLogEvents.CPR_RECORD_UPDATED)
    }
  }
}
