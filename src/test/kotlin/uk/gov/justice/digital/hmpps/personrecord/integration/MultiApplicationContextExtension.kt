package uk.gov.justice.digital.hmpps.personrecord.integration

import com.microsoft.applicationinsights.TelemetryClient
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource
import org.mockito.Mockito.spy
import org.springframework.boot.builder.SpringApplicationBuilder
import uk.gov.justice.digital.hmpps.personrecord.HmppsPersonRecord

class MultiApplicationContextExtension : BeforeAllCallback, CloseableResource, BeforeEachCallback {

  val instance1: SpringApplicationBuilder = SpringApplicationBuilder(HmppsPersonRecord::class.java)
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

  override fun beforeEach(context: ExtensionContext) {
    val telemetryClient = instance1.context().getBean("telemetryClient", TelemetryClient::class.java)
    val spyClient = spy(telemetryClient)
    (context.testInstance.get() as MessagingMultiNodeTestBase).otherTelemetryClient = spyClient
  }

  override fun close() {
    instance1.context().close()
  }

  companion object {
    private var started = false
  }
}
