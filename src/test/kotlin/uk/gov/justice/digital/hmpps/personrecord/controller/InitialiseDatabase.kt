package uk.gov.justice.digital.hmpps.personrecord.controller
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.IntegrationTestBase

class InitialiseDatabase : IntegrationTestBase() {

  @Test
  fun `initialises database`() {
    println("Database has been initialised by IntegrationTestBase")
  }
}
