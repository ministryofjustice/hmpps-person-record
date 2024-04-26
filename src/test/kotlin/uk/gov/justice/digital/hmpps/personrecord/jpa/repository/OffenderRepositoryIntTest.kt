package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OffenderEntity
import java.util.*
import kotlin.test.assertNotNull

class OffenderRepositoryIntTest : IntegrationTestBase() {

  @Test
  fun `should save offender successfully`() {
    val offenderEntity = OffenderEntity(
      crn = "E363876",
    )

    offenderRepository.saveAndFlush(offenderEntity)

    val createdOffender = offenderRepository.findByCrn("E363876")

    assertNotNull(createdOffender)
  }
}
