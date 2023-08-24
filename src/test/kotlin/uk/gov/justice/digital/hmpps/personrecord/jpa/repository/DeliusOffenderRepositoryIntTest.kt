package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DeliusOffenderEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeliusOffenderRepositoryIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var deliusOffenderRepository: DeliusOffenderRepository
  @Autowired
  lateinit var personRepository: PersonRepository
  @BeforeEach
  fun setUp() {
    deliusOffenderRepository.deleteAll()
  }
  @Test
  fun `should save offender successfully and link a new person record`() {
    val personId = UUID.randomUUID()
    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val deliusOffenderEntity = DeliusOffenderEntity(
      crn = "E363876",
      person = personEntity,

      )

    deliusOffenderEntity.createdBy = "test"
    deliusOffenderEntity.lastUpdatedBy = "test"

    deliusOffenderRepository.save(deliusOffenderEntity)

    val createdOffender = deliusOffenderRepository.findByCrn("E363876")

    assertNotNull(createdOffender)
    assertNotNull(createdOffender.person)
    assertEquals(personId, createdOffender.person!!.personId)
  }
  @Test
  fun ` should save offender successfully and link to an existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val existingPerson = personRepository.save(personEntity)

    val deliusOffenderEntity = DeliusOffenderEntity(
      crn = "E363880",
      person = existingPerson,
    )

    deliusOffenderEntity.createdBy = "test"
    deliusOffenderEntity.lastUpdatedBy = "test"

    existingPerson.deliusOffenders = mutableListOf(deliusOffenderEntity)

    personRepository.save(existingPerson)

    assertNotNull(deliusOffenderRepository.findByCrn("E363880"))
  }
   @Test
  fun ` should update offender list successfully for the existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    var existingPerson = personRepository.save(personEntity)

    val deliusOffenderEntity = DeliusOffenderEntity(
      crn = "E363881",
      person = existingPerson,

      )

    deliusOffenderEntity.createdBy = "test"
    deliusOffenderEntity.lastUpdatedBy = "test"

    existingPerson.deliusOffenders = mutableListOf(deliusOffenderEntity)

    personRepository.save(existingPerson)


    val existingOffender = deliusOffenderRepository.findByCrn("E363881")

    existingPerson  = existingOffender?.person!!

    val anotherDeliusOffenderEntity = DeliusOffenderEntity(
      crn = "E363999",
      person = existingPerson,

      )

    anotherDeliusOffenderEntity.createdBy = "test"
    anotherDeliusOffenderEntity.lastUpdatedBy = "test"

     existingPerson.deliusOffenders.add(anotherDeliusOffenderEntity)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.deliusOffenders.size)
    assertNotNull(deliusOffenderRepository.findByCrn("E363999"))
    assertNotNull(deliusOffenderRepository.findByCrn("E363881"))
  }

  @Test
  fun ` should return true for an existing offender`() {
    val personId = UUID.randomUUID()
    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val deliusOffenderEntity = DeliusOffenderEntity(
      crn = "E363876",
      person = personEntity,

      )

    deliusOffenderEntity.createdBy = "test"
    deliusOffenderEntity.lastUpdatedBy = "test"

    deliusOffenderRepository.save(deliusOffenderEntity)

    assertTrue { deliusOffenderRepository.existsByCrn("E363876") }
  }
  @Test
  fun `should return false for an unknown crn`() {

    assertFalse{ deliusOffenderRepository.existsByCrn("ABCD") }
  }
}