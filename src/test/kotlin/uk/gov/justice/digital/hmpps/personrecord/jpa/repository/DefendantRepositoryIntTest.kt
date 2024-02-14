package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.validate.PNCIdentifier
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DefendantRepositoryIntTest : IntegrationTestBase() {

  @Test
  fun `should save defendant successfully and link a new  person record`() {
    val personEntity = PersonEntity(
      personId = UUID.randomUUID(),

    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val defendantEntity = DefendantEntity(
      crn = "E363876",
      forenameOne = "Guinevere",
      surname = "Atherton",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "c04d3d2d-4bd2-40b9-bda6-564a4d9adb91",
      person = personEntity,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    defendantRepository.save(defendantEntity)

    assertNotNull(defendantRepository.findByDefendantId("c04d3d2d-4bd2-40b9-bda6-564a4d9adb91"))
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

    val defendantEntity = DefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "e59d442a-11c6-4fba-ace1-6d899ae5b9fa",
      person = existingPerson,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    existingPerson.defendants = mutableListOf(defendantEntity)

    personRepository.save(existingPerson)

    assertNotNull(defendantRepository.findByDefendantId("e59d442a-11c6-4fba-ace1-6d899ae5b9fa"))
  }

  @Test
  fun `should update defendant list successfully and link the new defendant to an existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    var existingPerson = personRepository.save(personEntity)

    val defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa"
    val defendantEntity = DefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = defendantId,
      person = existingPerson,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    existingPerson.defendants = mutableListOf(defendantEntity)

    personRepository.save(existingPerson)

    assertNotNull(defendantRepository.findByDefendantId(defendantId))

    val defendantEntity1 = defendantRepository.findByDefendantId(defendantId)

    existingPerson = defendantEntity1?.person!!

    val defendantEntity2 = DefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "b59d442a-11c6-4fba-ace1-6d899ae5b9za",
      person = existingPerson,
    )

    val addressEntity = AddressEntity(
      addressLineOne = "line 1",
      addressLineTwo = "line 2",
      postcode = "tw3 7pn",

    )
    addressEntity.createdBy = "test"
    addressEntity.lastUpdatedBy = "test"
    defendantEntity2.address = addressEntity

    defendantEntity2.createdBy = "test"
    defendantEntity2.lastUpdatedBy = "test"

    existingPerson.defendants.add(defendantEntity2)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.defendants.size)
    assertNotNull(defendantRepository.findByDefendantId(defendantId))
    assertNotNull(defendantRepository.findByDefendantId("b59d442a-11c6-4fba-ace1-6d899ae5b9za"))
  }

  @Test
  fun `should return correct defendant for a pnc`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val existingPerson = personRepository.save(personEntity)

    val pncNumber = "2003/0062845E"
    val defendantEntity = DefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "cbee7d12-5515-483f-8cdc-f4b03867c529",
      pncNumber = PNCIdentifier.from(pncNumber),
      person = existingPerson,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    val defendantEntity2 = DefendantEntity(
      forenameOne = "Rodney Another",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "69c94c02-9b7d-439f-9412-d0812f8c2835",
      pncNumber = PNCIdentifier.from("1981/0154257C"),
      person = existingPerson,
    )

    defendantEntity2.createdBy = "test"
    defendantEntity2.lastUpdatedBy = "test"

    existingPerson.defendants = mutableListOf(defendantEntity, defendantEntity2)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.defendants.size)

    val defendants = defendantRepository.findAllByPncNumber(PNCIdentifier.from(pncNumber))

    assertEquals(1, defendants.size)
    assertEquals(defendantEntity.pncNumber, defendants[0].pncNumber)
  }

  @Test
  fun `should return all defendants for a pnc`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val existingPerson = personRepository.save(personEntity)

    val defendantEntity = DefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "a6704fae-34ac-4ec6-9c6a-93afa73a85d9",
      pncNumber = PNCIdentifier.from("20030011985X"),
      person = existingPerson,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    val defendantEntity2 = DefendantEntity(
      forenameOne = "Rodney Another",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "0c4b2734-5bca-4401-a20a-90654f8a5a89",
      pncNumber = PNCIdentifier.from("20030011985X"),
      person = existingPerson,
    )

    defendantEntity2.createdBy = "test"
    defendantEntity2.lastUpdatedBy = "test"

    existingPerson.defendants = mutableListOf(defendantEntity, defendantEntity2)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.defendants.size)

    val defendants = defendantRepository.findAllByPncNumber(PNCIdentifier.from("20030011985X"))

    assertEquals(2, defendants.size)

    assertThat(defendants)
      .extracting("pncNumber")
      .containsOnly(PNCIdentifier.from("20030011985X"))
  }

  @Test
  fun `should persist new defendant with address and link to an existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    var existingPerson = personRepository.save(personEntity)

    val defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa"
    val defendantEntity = DefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = defendantId,
      person = existingPerson,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    existingPerson.defendants = mutableListOf(defendantEntity)

    personRepository.save(existingPerson)

    assertNotNull(defendantRepository.findByDefendantId(defendantId))

    val defendantEntity1 = defendantRepository.findByDefendantId(defendantId)

    existingPerson = defendantEntity1?.person!!

    val defendantEntity2 = DefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "b59d442a-11c6-4fba-ace1-6d899ae5b9za",
      person = existingPerson,
    )

    val addressEntity = AddressEntity(
      addressLineOne = "line 1",
      addressLineTwo = "line 2",
      postcode = "tw3 7pn",

    )
    addressEntity.createdBy = "test"
    addressEntity.lastUpdatedBy = "test"
    defendantEntity2.address = addressEntity

    defendantEntity2.createdBy = "test"
    defendantEntity2.lastUpdatedBy = "test"

    existingPerson.defendants.add(defendantEntity2)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.defendants.size)

    val defendantWithAddress = defendantRepository.findByDefendantId("b59d442a-11c6-4fba-ace1-6d899ae5b9za")
    assertNotNull(defendantWithAddress?.address)
    assertEquals(defendantWithAddress?.address?.postcode, "tw3 7pn")
  }
}
