package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.model.identifiers.PNCIdentifier
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DefendantRepositoryIntTest : IntegrationTestBase() {

  @Test
  fun `should return correct defendant for a pnc`() {
    val pncNumber = "2003/0062845E"
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "cbee7d12-5515-483f-8cdc-f4b03867c529",
      pncNumber = PNCIdentifier.from(pncNumber),
    )

    val defendantEntity2 = DefendantEntity(
      firstName = "Rodney Another",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "69c94c02-9b7d-439f-9412-d0812f8c2835",
      pncNumber = PNCIdentifier.from("1981/0154257C"),
    )

    val defendantEntities = mutableListOf(defendantEntity, defendantEntity2)

    defendantRepository.saveAll(defendantEntities)

    val defendants = defendantRepository.findAllByPncNumber(PNCIdentifier.from(pncNumber))

    assertEquals(1, defendants.size)
    assertEquals(defendantEntity.pncNumber, defendants[0].pncNumber)
  }

  @Test
  fun `should persist new defendant with address and contact`() {
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "b59d442a-11c6-4fba-ace1-6d899ae5b9za",
    )

    val addressEntity = AddressEntity(
      addressLineOne = "line 1",
      addressLineTwo = "line 2",
      postcode = "tw3 7pn",
    )
    defendantEntity.address = addressEntity

    val contactEntity = ContactEntity(
      homePhone = "02920675843",
      workPhone = "02920787665",
      mobile = "0767678766",
      primaryEmail = "email@email.com",
    )

    defendantEntity.contact = contactEntity

    defendantRepository.saveAndFlush(defendantEntity)

    val defendantWithAddressAndContact = defendantRepository.findByDefendantId("b59d442a-11c6-4fba-ace1-6d899ae5b9za")
    assertNotNull(defendantWithAddressAndContact?.address)
    assertEquals(defendantWithAddressAndContact?.address?.postcode, "tw3 7pn")
    assertNotNull(defendantWithAddressAndContact?.contact)
    assertEquals(defendantWithAddressAndContact?.contact?.mobile, "0767678766")
  }

  @Test
  fun `should update existing defendant with aliases and update the version`() {
    val defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa"
    val defendantEntity = DefendantEntity(
      firstName = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = defendantId,
    )

    val defendant = defendantRepository.saveAndFlush(defendantEntity)

    // adding aliases
    val aliasEntity = DefendantAliasEntity(
      firstName = "Dave",
      defendant = defendantEntity,
    )

    assertEquals(0, defendant.version)

    defendant.aliases = mutableListOf(aliasEntity)

    defendantRepository.saveAndFlush(defendant)

    val updatedDefendant = defendantRepository.findByDefendantId("a59d442a-11c6-4fba-ace1-6d899ae5b9fa")

    assertEquals(1, updatedDefendant?.version)
    assertEquals(1, updatedDefendant?.aliases?.size)
    assertEquals(updatedDefendant?.aliases!![0].firstName, "Dave")
  }
}
