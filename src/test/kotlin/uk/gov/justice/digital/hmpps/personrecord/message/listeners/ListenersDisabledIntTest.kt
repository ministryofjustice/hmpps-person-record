package uk.gov.justice.digital.hmpps.personrecord.message.listeners

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase
import kotlin.test.fail

@ActiveProfiles("seeding")
class ListenersDisabledIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var applicationContext: ApplicationContext

  @Test
  fun `should not have CourtCaseEventsListener bean when seeding profile is active`() {
    try {
      applicationContext.getBean(CourtCaseEventsListener::class.java)
      fail("Should have thrown an error")
    } catch (e: NoSuchBeanDefinitionException) {
      // expected
    }
  }

  @Test
  fun `should not have ProbationEventListener bean when seeding profile is active`() {
    try {
      applicationContext.getBean(ProbationEventListener::class.java)
      fail("Should have thrown an error")
    } catch (e: NoSuchBeanDefinitionException) {
      // expected
    }
  }

  @Test
  fun `should not have PrisonEventListener bean when seeding profile is active`() {
    try {
      applicationContext.getBean(PrisonEventListener::class.java)
      fail("Should have thrown an error")
    } catch (e: NoSuchBeanDefinitionException) {
      // expected
    }
  }
}
