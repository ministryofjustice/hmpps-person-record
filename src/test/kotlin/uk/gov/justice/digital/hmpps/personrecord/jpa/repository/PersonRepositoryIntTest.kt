package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlConfig
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import java.time.LocalDate
import java.util.*

@Sql(
  scripts = ["classpath:sql/before-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
)
@Sql(
  scripts = ["classpath:sql/after-test.sql"],
  config = SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
  executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
)
class PersonRepositoryIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var personRepository: PersonRepository

  @Test
  fun `should return exact match for provided UUID`() {
    // Given
    val personId = UUID.fromString("eed4a9a4-d853-11ed-afa1-0242ac120002")

    // When
    val personEntity = personRepository.findByPersonId(personId)

    // Then
    assertThat(personEntity).isNotNull
    assertThat(personEntity?.hmctsDefendants).hasSize(2)
    val hmctsDefendantEntity = personEntity?.hmctsDefendants?.get(0)
    assertThat(hmctsDefendantEntity?.pncNumber).isEqualTo("PNC12345")
    assertThat(hmctsDefendantEntity?.crn).isEqualTo("CRN1234")
    assertThat(hmctsDefendantEntity?.forenameOne).isEqualTo("Iestyn")
    assertThat(hmctsDefendantEntity?.forenameTwo).isEqualTo("Carey")
    assertThat(hmctsDefendantEntity?.surname).isEqualTo("Mahoney")
    assertThat(hmctsDefendantEntity?.dateOfBirth).isEqualTo(LocalDate.of(1965, 6, 18))
  }

  @Test
  fun `should return null match for non existent UUID`() {
    // Given
    val personId = UUID.fromString("b0ff6dec-2706-11ee-be56-0242ac120002")

    // When
    val personEntity = personRepository.findByPersonId(personId)

    // Then
    assertThat(personEntity).isNull()
  }

  @Test
  fun `should return linked delius offender entity for existing person`() {
    // Given
    val personId = UUID.fromString("d75a9374-e2a3-11ed-b5ea-0242ac120002")

    // When
    val personEntity = personRepository.findByPersonId(personId)

    // Then
    assertThat(personEntity).isNotNull
    assertThat(personEntity?.deliusOffenders).hasSize(1)
    assertThat(personEntity?.deliusOffenders?.get(0)?.crn).isEqualTo("CRN1234")
  }

  @Test
  fun `should return null for unknown CRN`() {
    // Given
    val crn = "CRN9999"

    // When
    val personEntity = personRepository.findByDeliusOffendersCrn(crn)

    // Then
    assertThat(personEntity).isNull()
  }
}
