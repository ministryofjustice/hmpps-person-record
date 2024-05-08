package uk.gov.justice.digital.hmpps.personrecord.integration

import org.junit.jupiter.api.Test
import org.testcontainers.containers.GenericContainer

class SchemaGenerationIntTest : IntegrationTestBase() {

  @Test
  fun `should generate schema`() {
    GenericContainer<Nothing>("schemaspy/schemaspy:6.2.4").apply {
      withCreateContainerCmdModifier { it.withEntrypoint("") }
      withCommand("sleep 300000")
    }.use { schemaSpy ->
      schemaSpy.start()
      schemaSpy.execInContainer(
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

      schemaSpy.execInContainer("tar", "-czvf", "/output/output.tar.gz", "/output")

      schemaSpy.copyFileFromContainer(
        "/output/output.tar.gz",
        "build/reports/schemaspy.tar.gz",
      )
    }
    ProcessBuilder().command("unzip", "build/reports/schemaspy.tar.gz")
  }
}
