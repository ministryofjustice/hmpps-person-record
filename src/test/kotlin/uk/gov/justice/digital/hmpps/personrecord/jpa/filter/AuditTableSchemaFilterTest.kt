package uk.gov.justice.digital.hmpps.personrecord.jpa.filter

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.mapping.Table
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class AuditTableSchemaFilterTest {

  private val filter: AuditTableSchemaFilter = AuditTableSchemaFilter()

  @ParameterizedTest
  @ValueSource(strings = ["person_aud", "revinfo", "REVINFO", "PERSON_AUD, some_table_aud"])
  fun `should return true for provided table name`(tableName: String) {
    assertThat(filter.includeTable(Table(null, tableName))).isTrue
  }

  @ParameterizedTest
  @ValueSource(strings = ["person", "offender", "alias", "PERSON"])
  fun `should return false for provided table name`(tableName: String) {
    assertThat(filter.includeTable(Table(null, tableName))).isFalse
  }
}
