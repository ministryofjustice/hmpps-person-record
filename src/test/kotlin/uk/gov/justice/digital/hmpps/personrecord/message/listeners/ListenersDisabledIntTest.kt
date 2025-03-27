package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.court.CourtEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison.PrisonEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.prison.PrisonMergeEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationDeletionEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationEventListener
import uk.gov.justice.digital.hmpps.personrecord.message.listeners.probation.ProbationMergeEventListener
import kotlin.test.assertFailsWith

@ActiveProfiles("seeding")
class ListenersDisabledIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var applicationContext: ApplicationContext

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
  fun `should not have ProbationDeleteEventListener bean when seeding profile is active`() {
    assertFailsWith<NoSuchBeanDefinitionException>(
      block = { applicationContext.getBean(ProbationDeletionEventListener::class.java) },
    )
  }

  @Test
  fun `should not have ProbationMergeEventListener bean when seeding profile is active`() {
    assertFailsWith<NoSuchBeanDefinitionException>(
      block = { applicationContext.getBean(ProbationMergeEventListener::class.java) },
    )
  }

  @Test
  fun `should not have PrisonEventListener bean when seeding profile is active`() {
    assertFailsWith<NoSuchBeanDefinitionException>(
      block = { applicationContext.getBean(PrisonEventListener::class.java) },
    )
  }

  @Test
  fun `should not have PrisonMargeEventListener bean when seeding profile is active`() {
    assertFailsWith<NoSuchBeanDefinitionException>(
      block = { applicationContext.getBean(PrisonMergeEventListener::class.java) },
    )
  }
}
