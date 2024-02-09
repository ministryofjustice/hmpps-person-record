package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OffenderRepositoryIntTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    offenderRepository.deleteAll()
  }

  @Test
  fun `should save offender successfully and link a new person record`() {
    val personId = UUID.randomUUID()
    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val offenderEntity = OffenderEntity(
      crn = "E363876",
      person = personEntity,

    )

    offenderEntity.createdBy = "test"
    offenderEntity.lastUpdatedBy = "test"

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
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val existingPerson = personRepository.save(personEntity)

    val offenderEntity = OffenderEntity(
      crn = "E363880",
      person = existingPerson,
    )

    offenderEntity.createdBy = "test"
    offenderEntity.lastUpdatedBy = "test"

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
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    var existingPerson = personRepository.save(personEntity)

    val offenderEntity = OffenderEntity(
      crn = "E363881",
      person = existingPerson,

    )

    offenderEntity.createdBy = "test"
    offenderEntity.lastUpdatedBy = "test"

    existingPerson.offenders = mutableListOf(offenderEntity)

    personRepository.save(existingPerson)

    val existingOffender = offenderRepository.findByCrn("E363881")

    existingPerson = existingOffender?.person!!

    val anotherOffenderEntity = OffenderEntity(
      crn = "E363999",
      person = existingPerson,

    )

    anotherOffenderEntity.createdBy = "test"
    anotherOffenderEntity.lastUpdatedBy = "test"

    existingPerson.offenders.add(anotherOffenderEntity)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.offenders.size)
    assertNotNull(offenderRepository.findByCrn("E363999"))
    assertNotNull(offenderRepository.findByCrn("E363881"))
  }

  @Test
  fun `should return true for an existing offender`() {
    val personId = UUID.randomUUID()
    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val offenderEntity = OffenderEntity(
      crn = "E363876",
      person = personEntity,

    )

    offenderEntity.createdBy = "test"
    offenderEntity.lastUpdatedBy = "test"

    offenderRepository.save(offenderEntity)

    assertTrue { offenderRepository.existsByCrn("E363876") }
  }

  @Test
  fun `should return false for an unknown crn`() {
    assertFalse { offenderRepository.existsByCrn("ABCD") }
  }
}
