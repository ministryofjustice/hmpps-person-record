package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.PNCIdentifier
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

    val defendantEntity = DefendantEntity(
      crn = "E363876",
      firstName = "Guinevere",
      surname = "Atherton",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "c04d3d2d-4bd2-40b9-bda6-564a4d9adb91",
      person = personEntity,
    )

    defendantRepository.save(defendantEntity)

    assertNotNull(defendantRepository.findByDefendantId("c04d3d2d-4bd2-40b9-bda6-564a4d9adb91"))
  }

  @Test
  fun `should save offender successfully and link to an existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )

    val existingPerson = personRepository.save(personEntity)

    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "e59d442a-11c6-4fba-ace1-6d899ae5b9fa",
      person = existingPerson,
    )

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

    var existingPerson = personRepository.save(personEntity)

    val defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa"
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = defendantId,
      person = existingPerson,
    )

    existingPerson.defendants = mutableListOf(defendantEntity)

    personRepository.save(existingPerson)

    assertNotNull(defendantRepository.findByDefendantId(defendantId))

    val defendantEntity1 = defendantRepository.findByDefendantId(defendantId)

    existingPerson = defendantEntity1?.person!!

    val defendantEntity2 = DefendantEntity(
      firstName = "Rodney",
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

    val existingPerson = personRepository.save(personEntity)

    val pncNumber = "2003/0062845E"
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "cbee7d12-5515-483f-8cdc-f4b03867c529",
      pncNumber = PNCIdentifier.from(pncNumber),
      person = existingPerson,
    )

    val defendantEntity2 = DefendantEntity(
      firstName = "Rodney Another",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "69c94c02-9b7d-439f-9412-d0812f8c2835",
      pncNumber = PNCIdentifier.from("1981/0154257C"),
      person = existingPerson,
    )

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

    val existingPerson = personRepository.save(personEntity)

    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "a6704fae-34ac-4ec6-9c6a-93afa73a85d9",
      pncNumber = PNCIdentifier.from("20030011985X"),
      person = existingPerson,
    )

    val defendantEntity2 = DefendantEntity(
      firstName = "Rodney Another",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "0c4b2734-5bca-4401-a20a-90654f8a5a89",
      pncNumber = PNCIdentifier.from("20030011985X"),
      person = existingPerson,
    )

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
  fun `should persist new defendant with address and contact and link to an existing person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )

    var existingPerson = personRepository.save(personEntity)

    val defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa"
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = defendantId,
      person = existingPerson,
    )

    existingPerson.defendants = mutableListOf(defendantEntity)

    personRepository.save(existingPerson)

    assertNotNull(defendantRepository.findByDefendantId(defendantId))

    val defendantEntity1 = defendantRepository.findByDefendantId(defendantId)

    existingPerson = defendantEntity1?.person!!

    val defendantEntity2 = DefendantEntity(
      firstName = "Rodney",
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
    defendantEntity2.address = addressEntity

    val contactEntity = ContactEntity(
      homePhone = "02920675843",
      workPhone = "02920787665",
      mobile = "0767678766",
      primaryEmail = "email@email.com",
    )

    defendantEntity2.contact = contactEntity

    existingPerson.defendants.add(defendantEntity2)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.defendants.size)

    val defendantWithAddressAndContact = defendantRepository.findByDefendantId("b59d442a-11c6-4fba-ace1-6d899ae5b9za")
    assertNotNull(defendantWithAddressAndContact?.address)
    assertEquals(defendantWithAddressAndContact?.address?.postcode, "tw3 7pn")
    assertNotNull(defendantWithAddressAndContact?.contact)
    assertEquals(defendantWithAddressAndContact?.contact?.mobile, "0767678766")
  }

  @Test
  fun `should persist new defendant with aliases and create a person record`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )

    val defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa"
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = defendantId,
      person = personEntity,
    )

    val aliasEntity = DefendantAliasEntity(
      firstName = "Dave",
      defendant = defendantEntity,
    )

    defendantEntity.aliases = mutableListOf(aliasEntity)

    personEntity.defendants = mutableListOf(defendantEntity)

    val newPerson = personRepository.save(personEntity)

    assertEquals(1, newPerson.defendants.size)

    val defendantWithAliases = newPerson.defendants[0]

    assertEquals(1, defendantWithAliases.aliases?.size)
    assertEquals(defendantWithAliases.aliases!![0].firstName, "Dave")
  }

  @Test
  fun `should update existing defendant with aliases and update the version`() {
    val personId = UUID.randomUUID()

    val personEntity = PersonEntity(
      personId = personId,
    )

    val defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa"
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = defendantId,
      person = personEntity,
    )

    personEntity.defendants = mutableListOf(defendantEntity)

    val person = personRepository.save(personEntity)

    assertEquals(1, person.defendants.size)

    // adding aliases
    val aliasEntity = DefendantAliasEntity(
      firstName = "Dave",
      defendant = defendantEntity,
    )

    val existingDefendant = person.defendants[0]
    assertEquals(0, existingDefendant.version)

    existingDefendant.aliases = mutableListOf(aliasEntity)

    person.defendants.add(existingDefendant)

    personRepository.saveAndFlush(person)

    val updatedDefendant = defendantRepository.findByDefendantId("a59d442a-11c6-4fba-ace1-6d899ae5b9fa")

    assertEquals(1, updatedDefendant?.version)
    assertEquals(1, updatedDefendant?.aliases?.size)
    assertEquals(updatedDefendant?.aliases!![0].firstName, "Dave")
  }
}
