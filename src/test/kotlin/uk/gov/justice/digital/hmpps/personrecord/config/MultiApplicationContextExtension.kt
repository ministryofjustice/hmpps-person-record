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

  companion object {
    private val NAMESPACE = ExtensionContext.Namespace.create(MultiApplicationContextExtension::class.java)
    private const val CONTEXT = "secondary-context"
  }

  override fun beforeAll(context: ExtensionContext) {
    val store = context.getStore(NAMESPACE)

    // This test class/container already has its secondary context.
    if (store.get(CONTEXT) != null) {
      return
    }

    val primaryContext = SpringExtension.getApplicationContext(context)

    val secondaryContext = SpringApplicationBuilder(HmppsPersonRecord::class.java)
      .profiles(*primaryContext.environment.activeProfiles, "test-instance-1")
      .run()

    store.put(CONTEXT, secondaryContext)
  }

  override fun afterAll(context: ExtensionContext) {
    context.getStore(NAMESPACE)
      .remove(CONTEXT, ConfigurableApplicationContext::class.java)
      ?.close()
  }
}
