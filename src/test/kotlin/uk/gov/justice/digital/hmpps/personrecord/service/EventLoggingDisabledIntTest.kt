package uk.gov.justice.digital.hmpps.personrecord.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("local")
class EventLoggingDisabledIntTest {

  @Autowired
  lateinit var applicationContext: ApplicationContext

  @Test
  fun `should find EventLoggingServiceNoOp bean`() {
    val getService = applicationContext.getBean(EventLoggingService::class.java)
    assertThat(getService is EventLoggingServiceNoOp).isTrue()
  }
}
