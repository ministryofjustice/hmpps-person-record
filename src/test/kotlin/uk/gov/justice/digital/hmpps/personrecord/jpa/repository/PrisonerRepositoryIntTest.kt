package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonerAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PrisonerEntity
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PrisonerRepositoryIntTest : IntegrationTestBase() {

  @Test
  fun `should save prisoner successfully and link a new person record`() {
    val personId = UUID.randomUUID()
    val personEntity = PersonEntity(
      personId = personId,
    )

    val prisonerEntity = PrisonerEntity(
      prisonNumber = "A1234BB",
      firstName = "Rodney",
      lastName = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      person = personEntity,

    )

    prisonerRepository.save(prisonerEntity)

    val createdPrisoner = prisonerRepository.findByPrisonNumber("A1234BB")

    assertNotNull(createdPrisoner)
    assertNotNull(createdPrisoner.person)
    assertEquals(personId, createdPrisoner.person!!.personId)
  }

  @Test
  fun `should save new prisoner with aliases and create a person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )

    val prisonerEntity = PrisonerEntity(
      prisonNumber = "B1234AA",
      firstName = "Rodney",
      lastName = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      person = personEntity,
    )

    val aliasEntity = PrisonerAliasEntity(
      firstName = "Dave",
      lastName = "Trotter",
      prisoner = prisonerEntity,
      dateOfBirth = LocalDate.of(1980, 5, 1),
    )

    prisonerEntity.aliases = mutableListOf(aliasEntity)

    personEntity.prisoners = mutableListOf(prisonerEntity)

    val newPerson = personRepository.save(personEntity)

    assertEquals(1, newPerson.prisoners.size)

    val prisonerWithAliases = newPerson.prisoners[0]

    assertEquals(1, prisonerWithAliases.aliases.size)
    assertEquals(prisonerWithAliases.aliases[0].firstName, "Dave")
  }

  @Test
  fun `should save prisoner successfully without a person record`() {
    val prisonerEntity = PrisonerEntity(
      prisonNumber = "A1234BB",
      firstName = "Rodney",
      lastName = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      )

    prisonerRepository.save(prisonerEntity)

    val createdPrisoner = prisonerRepository.findByPrisonNumber("A1234BB")

    assertNotNull(createdPrisoner)
    assertNull(createdPrisoner.person)
  }
}
