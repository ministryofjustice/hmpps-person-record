package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.test.randomPrisonNumber
import java.time.LocalDateTime

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringJUnitConfig(SysconSyncPseudonymHandlerImpl::class, SysconSyncPseudonymHandlerProd::class)
class SysconSyncPseudonymHandlerTest {
  private val prisonNumber = randomPrisonNumber()
  private val prisonAlias = PrisonAlias(
    nomisOffenderId = 10000L,
    isPrimary = true,
    createDateTime = LocalDateTime.of(2020, 1, 1, 12, 0),
    createUserId = "createUserId",
  )

  @MockitoBean
  private lateinit var personRepository: PersonRepository

  @MockitoBean
  private lateinit var personService: PersonService

  @Nested
  @ActiveProfiles("prod")
  inner class ProdProfileTest(@Autowired private val sysconSyncPseudonymHandler: SysconSyncPseudonymHandler) {

    @Test
    fun `uses the correct pseudonym handler for the active profile`() {
      assertThat(sysconSyncPseudonymHandler).isInstanceOf(SysconSyncPseudonymHandlerProd::class.java)
    }

    @Test
    fun `insert returns an empty alias mapping`() {
      val result = sysconSyncPseudonymHandler.handleInsert(prisonNumber, prisonAlias)

      assertThat(result).isEqualTo(SysconAliasMapping())
    }
  }

  @Nested
  @ActiveProfiles("preprod")
  inner class PreProdProfileTest(@Autowired private val sysconSyncPseudonymHandler: SysconSyncPseudonymHandler) {

    @Test
    fun `uses the correct pseudonym handler for the active profile`() {
      assertThat(sysconSyncPseudonymHandler).isInstanceOf(SysconSyncPseudonymHandlerProd::class.java)
    }

    @Test
    fun `insert returns an empty alias mapping`() {
      val result = sysconSyncPseudonymHandler.handleInsert(prisonNumber, prisonAlias)

      assertThat(result).isEqualTo(SysconAliasMapping())
    }
  }

  @Nested
  @ActiveProfiles("dev")
  inner class DevProfileTest(@Autowired private val sysconSyncPseudonymHandler: SysconSyncPseudonymHandler) {

    @Test
    fun `uses the correct pseudonym handler for the active profile`() {
      assertThat(sysconSyncPseudonymHandler).isInstanceOf(SysconSyncPseudonymHandlerProd::class.java)
    }

    @Test
    fun `insert returns an empty alias mapping`() {
      val result = sysconSyncPseudonymHandler.handleInsert(prisonNumber, prisonAlias)

      assertThat(result).isEqualTo(SysconAliasMapping())
    }
  }

  @Nested
  @ActiveProfiles("test")
  inner class TestProfileTest(@Autowired private val sysconSyncPseudonymHandler: SysconSyncPseudonymHandler) {

    @Test
    fun `uses the correct pseudonym handler for the active profile`() {
      assertThat(sysconSyncPseudonymHandler).isInstanceOf(SysconSyncPseudonymHandlerImpl::class.java)
    }

    @Test
    fun `insert creates a person when the person does not exist`() {
      whenever(personRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
      whenever(personService.processCreate(eq(NOMIS), any())).thenReturn(PersonEntity.new(NOMIS))

      sysconSyncPseudonymHandler.handleInsert(prisonNumber, prisonAlias)

      verify(personService).processCreate(eq(NOMIS), any())
    }
  }
}
