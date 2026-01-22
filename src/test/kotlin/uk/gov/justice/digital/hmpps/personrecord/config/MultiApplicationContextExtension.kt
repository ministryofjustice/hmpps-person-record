package uk.gov.justice.digital.hmpps.personrecord.config

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.boot.builder.SpringApplicationBuilder
import uk.gov.justice.digital.hmpps.personrecord.HmppsPersonRecord

class MultiApplicationContextExtension :
  BeforeAllCallback,
  AutoCloseable {

  private val instance1: SpringApplicationBuilder = SpringApplicationBuilder(HmppsPersonRecord::class.java)
    .profiles("test", "test-instance-1")

  override fun beforeAll(context: ExtensionContext) {
    if (!started) {
      started = true
      instance1.run()

      context.root.getStore(ExtensionContext.Namespace.GLOBAL).put(
        "test-instance-extension",
        this,
      )
    }
  }

  override fun close() {
    instance1.context()?.close()
  }

  companion object {
    private var started = false
  }
}
