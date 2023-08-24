package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.HmctsDefendantEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HmctsDefendantRepositoryIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var hmctsDefendantRepository: HmctsDefendantRepository

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `should save defendant successfully and link a new  person record`() {
    val personEntity = PersonEntity(
      personId = UUID.randomUUID(),

    )
    personEntity.createdBy = "test"
    personEntity.lastUpdatedBy = "test"

    val defendantEntity = HmctsDefendantEntity(
      crn = "E363876",
      forenameOne = "Guinevere",
      surname = "Atherton",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "c04d3d2d-4bd2-40b9-bda6-564a4d9adb91",
      person = personEntity,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    hmctsDefendantRepository.save(defendantEntity)

    assertNotNull(hmctsDefendantRepository.findByDefendantId("c04d3d2d-4bd2-40b9-bda6-564a4d9adb91"))
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

    val defendantEntity = HmctsDefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "e59d442a-11c6-4fba-ace1-6d899ae5b9fa",
      person = existingPerson,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    existingPerson.hmctsDefendants = mutableListOf(defendantEntity)

    personRepository.save(existingPerson)

    assertNotNull(hmctsDefendantRepository.findByDefendantId("e59d442a-11c6-4fba-ace1-6d899ae5b9fa"))
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

    val defendantEntity = HmctsDefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "a59d442a-11c6-4fba-ace1-6d899ae5b9fa",
      person = existingPerson,
    )

    defendantEntity.createdBy = "test"
    defendantEntity.lastUpdatedBy = "test"

    existingPerson.hmctsDefendants = mutableListOf(defendantEntity)

    personRepository.save(existingPerson)

    assertNotNull(hmctsDefendantRepository.findByDefendantId("a59d442a-11c6-4fba-ace1-6d899ae5b9fa"))

    val defendantEntity1 = hmctsDefendantRepository.findByDefendantId("a59d442a-11c6-4fba-ace1-6d899ae5b9fa")

    existingPerson = defendantEntity1?.person!!

    val defendantEntity2 = HmctsDefendantEntity(
      forenameOne = "Rodney",
      surname = "Trotter",
      dateOfBirth = LocalDate.of(1980, 5, 1),
      defendantId = "b59d442a-11c6-4fba-ace1-6d899ae5b9za",
      person = existingPerson,
    )

    defendantEntity2.createdBy = "test"
    defendantEntity2.lastUpdatedBy = "test"

    existingPerson.hmctsDefendants.add(defendantEntity2)

    val personEntityUpdated = personRepository.save(existingPerson)

    assertEquals(2, personEntityUpdated.hmctsDefendants.size)
    assertNotNull(hmctsDefendantRepository.findByDefendantId("e59d442a-11c6-4fba-ace1-6d899ae5b9fa"))
    assertNotNull(hmctsDefendantRepository.findByDefendantId("b59d442a-11c6-4fba-ace1-6d899ae5b9za"))
  }
}
