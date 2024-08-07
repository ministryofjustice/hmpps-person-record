package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.health.PersonMatchHealthPing
import uk.gov.justice.digital.hmpps.personrecord.health.PersonRecordHealthPing
import kotlin.test.assertFailsWith

@ActiveProfiles("seeding")
class ListenersDisabledIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var applicationContext: ApplicationContext

  @MockBean
  private lateinit var personMatchHealthPing: PersonMatchHealthPing

  @MockBean
  @Autowired
  private lateinit var personRecordHealthPing: PersonRecordHealthPing

  @Test
  fun `should not have CourtEventListener bean when seeding profile is active`() {
    assertFailsWith<NoSuchBeanDefinitionException>(
      block = { applicationContext.getBean(CourtEventListener::class.java) },
    )
  }

  @Test
  fun `should not have ProbationEventListener bean when seeding profile is active`() {
    assertFailsWith<NoSuchBeanDefinitionException>(
      block = { applicationContext.getBean(ProbationEventListener::class.java) },
    )
  }

  @Test
  fun `should not have PrisonEventListener bean when seeding profile is active`() {
    assertFailsWith<NoSuchBeanDefinitionException>(
      block = { applicationContext.getBean(PrisonEventListener::class.java) },
    )
  }
}
