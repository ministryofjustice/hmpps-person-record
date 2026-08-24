package uk.gov.justice.digital.hmpps.personrecord.config

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import uk.gov.justice.digital.hmpps.personrecord.HmppsPersonRecord

class MultiApplicationContextExtension :
  BeforeAllCallback,
  AfterAllCallback {

  private var instance1: ConfigurableApplicationContext? = null

  override fun beforeAll(context: ExtensionContext) {
    val existingContext = SpringExtension.getApplicationContext(context)

    instance1 = SpringApplicationBuilder(HmppsPersonRecord::class.java)
      .profiles(*existingContext.environment.activeProfiles, "test-instance-1")
      .run()
  }

  override fun afterAll(context: ExtensionContext) {
    instance1?.close()
    instance1 = null
  }
}
