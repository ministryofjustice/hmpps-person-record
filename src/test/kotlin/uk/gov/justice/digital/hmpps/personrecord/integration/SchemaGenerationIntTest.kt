package uk.gov.justice.digital.hmpps.personrecord.integration

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer
import java.nio.file.Files
import java.nio.file.Paths

class SchemaGenerationIntTest : IntegrationTestBase() {

  @Test
  fun `should generate schema`() {
    GenericContainer<Nothing>("schemaspy/schemaspy:6.2.4").apply {
      withCreateContainerCmdModifier { it.withEntrypoint("") }
      withCommand("sleep 300000")
    }.use { schemaSpy ->
      schemaSpy.start()
      val execInContainer = schemaSpy.execInContainer(
        "/usr/local/bin/schemaspy",
        "-host",
        "host.docker.internal",
        "-port",
        "5432",
        "-db",
        "postgres",
        "-t",
        "pgsql",
        "-s",
        "personrecordservicetest",
        "-u",
        "root",
        "-p",
        "dev",
      )
      println(execInContainer.stdout)
      println(execInContainer.stderr)
      println(execInContainer.exitCode)

      schemaSpy.execInContainer("tar", "-czvf", "/output/output.tar.gz", "/output")

      schemaSpy.copyFileFromContainer(
        "/output/output.tar.gz",
        "build/reports/schemaspy.tar.gz",
      )
    }
    ProcessBuilder().command("mkdir", "-p", "build/reports/schemaspy").start()
    ProcessBuilder().command("tar", "-xzvf", "build/reports/schemaspy.tar.gz", "-C", "build/reports/schemaspy").start()
    await untilAsserted {
      assertThat(Files.exists(Paths.get("build/reports/schemaspy/output/index.html"))).isTrue()
    }
  }
}
