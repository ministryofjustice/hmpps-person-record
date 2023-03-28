package uk.gov.justice.digital.hmpps.personrecord.jpa.filter

import org.hibernate.boot.model.relational.Namespace
import org.hibernate.boot.model.relational.Sequence
import org.hibernate.mapping.Table
import org.hibernate.tool.schema.spi.SchemaFilter

/**
 * Schema Filter implementation that ensures only Envers Audit tables (ending _aud) and the revision info table are automatically
 * created by Hibernate
 */
class AuditTableSchemaFilter : SchemaFilter {

  companion object {
    val INSTANCE = AuditTableSchemaFilter()
  }

  override fun includeNamespace(namespace: Namespace): Boolean {
    return true
  }

  override fun includeTable(table: Table): Boolean {
    return (
      table.name.endsWith("_aud", ignoreCase = true) ||
        table.name.equals("revinfo", ignoreCase = true)
      )
  }

  override fun includeSequence(sequence: Sequence): Boolean {
    return true
  }
}
