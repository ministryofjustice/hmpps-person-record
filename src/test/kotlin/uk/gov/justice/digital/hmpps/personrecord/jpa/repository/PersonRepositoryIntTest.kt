package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonAliasEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.time.LocalDate
import kotlin.test.assertNotNull

class PersonRepositoryIntTest : IntegrationTestBase() {

  @Test
  fun `should create a new person record`() {
    val personEntity = PersonEntity(
      title = "Mr",
      firstName = "John",
      lastName = "Smith",
      dateOfBirth = LocalDate.of(1970, 1, 1),
      sourceSystem = SourceSystemType.DELIUS,
    )

    val aliases = mutableListOf(
      PersonAliasEntity(
        firstName = "Jon",
        lastName = "Smyth",
        person = personEntity,
      ),
    )

    personEntity.aliases = aliases

    val createdPerson = personRepository.saveAndFlush(personEntity)

    assertNotNull(createdPerson)
    assertThat(createdPerson.aliases.size).isEqualTo(1)
  }
}
