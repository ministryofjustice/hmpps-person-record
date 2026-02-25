package uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SysconReligionResponseBodyTest {
  @Test
  fun `correctly maps response body`() {
    val prisonId = "1"
    val cprIdsByNomisId = mapOf("2" to "3", "4" to "5")

    val actual = SysconReligionResponseBody.from(prisonId, cprIdsByNomisId)
    assertThat(actual.prisonNumber).isEqualTo(prisonId)
    assertThat(actual.religionMappings.size).isEqualTo(2)
    assertThat(actual.religionMappings[0].nomisReligionId).isEqualTo(cprIdsByNomisId.entries.elementAt(0).key)
    assertThat(actual.religionMappings[0].cprReligionId).isEqualTo(cprIdsByNomisId.entries.elementAt(0).value)
    assertThat(actual.religionMappings[1].nomisReligionId).isEqualTo(cprIdsByNomisId.entries.elementAt(1).key)
    assertThat(actual.religionMappings[1].cprReligionId).isEqualTo(cprIdsByNomisId.entries.elementAt(1).value)
  }
}
