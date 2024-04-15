package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OffenderRepositoryIntTest : IntegrationTestBase() {

  @Test
  fun `should save offender successfully and link a new person record`() {
    val personId = UUID.randomUUID()
    val personEntity = PersonEntity(
      personId = personId,
    )

    val offenderEntity = OffenderEntity(
      crn = "E363876",
      person = personEntity,

    )

    offenderRepository.save(offenderEntity)

    val createdOffender = offenderRepository.findByCrn("E363876")

    assertNotNull(createdOffender)
    assertNotNull(createdOffender.person)
    assertEquals(personId, createdOffender.person!!.personId)
  }

  @Test
  fun `should save offender successfully and link to an existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )

    val existingPerson = personRepository.save(personEntity)

    val offenderEntity = OffenderEntity(
      crn = "E363880",
      person = existingPerson,
    )

    existingPerson.offenders = mutableListOf(offenderEntity)

    personRepository.save(existingPerson)

    assertNotNull(offenderRepository.findByCrn("E363880"))
  }

  @Test
  fun `should update offender list successfully for the existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )

    var existingPerson = personRepository.save(personEntity)

    val offenderEntity = OffenderEntity(
      crn = "E363881",
      person = existingPerson,

    )

    existingPerson.offenders = mutableListOf(offenderEntity)

    personRepository.save(existingPerson)

    val existingOffender = offenderRepository.findByCrn("E363881")

    existingPerson = existingOffender?.person!!

    val anotherOffenderEntity = OffenderEntity(
      crn = "E363999",
      person = existingPerson,

    )

    existingPerson.offenders.add(anotherOffenderEntity)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.offenders.size)
    assertNotNull(offenderRepository.findByCrn("E363999"))
    assertNotNull(offenderRepository.findByCrn("E363881"))
  }

  @Test
  fun `should save offender successfully without a person record`() {
    val offenderEntity = OffenderEntity(
      crn = "E363876",
    )

    offenderRepository.save(offenderEntity)

    val createdOffender = offenderRepository.findByCrn("E363876")

    assertNotNull(createdOffender)
    assertNull(createdOffender.person)
  }
}
