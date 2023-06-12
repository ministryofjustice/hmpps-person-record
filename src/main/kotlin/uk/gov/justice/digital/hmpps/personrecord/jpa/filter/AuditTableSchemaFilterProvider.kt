package uk.gov.justice.digital.hmpps.personrecord.jpa.filter

import org.hibernate.tool.schema.spi.SchemaFilter
import org.hibernate.tool.schema.spi.SchemaFilterProvider
import org.springframework.stereotype.Component

@Component
class AuditTableSchemaFilterProvider : SchemaFilterProvider {
  override fun getCreateFilter(): SchemaFilter {
    return AuditTableSchemaFilter.INSTANCE
  }

  override fun getDropFilter(): SchemaFilter {
    return AuditTableSchemaFilter.INSTANCE
  }

  override fun getTruncatorFilter(): SchemaFilter {
    return AuditTableSchemaFilter.INSTANCE
  }

  override fun getMigrateFilter(): SchemaFilter {
    return AuditTableSchemaFilter.INSTANCE
  }

  override fun getValidateFilter(): SchemaFilter {
    return AuditTableSchemaFilter.INSTANCE
  }
}
