package uk.gov.justice.digital.hmpps.personrecord.builders

import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.queries.PersonQuery

class SQLBuilder {

  private val query = mutableListOf<String>()

  fun select(vararg columns: String?): SQLBuilder {
    when {
      columns.isNotEmpty() -> {
        query.add("SELECT")
        query.add(columns.joinToString(", "))
      }
      else -> query.add("*")
    }
    return this
  }

  fun from(table: String, alias: String? = ""): SQLBuilder {
    query.add("FROM $table $alias".trim())
    return this
  }

  fun where(condition: String): SQLBuilder {
    query.add("WHERE")
    query.add(condition)
    return this
  }

  fun innerJoin(table: String, alias: String?): SQLBuilder {
    query.add("INNER JOIN $table $alias".trim())
    return this
  }

  fun on(condition: String): SQLBuilder {
    query.add("ON")
    query.add(condition)
    return this
  }

  fun and(condition: String): SQLBuilder {
    query.add("AND")
    query.add(condition)
    return this
  }

  fun or(condition: String): SQLBuilder {
    query.add("OR")
    query.add(condition)
    return this
  }

  fun append(sqlBuilder: SQLBuilder): SQLBuilder {
    query.add(sqlBuilder.build())
    return this
  }

  fun build(): String {
    return query.joinToString(DELIMITER).trim()
  }

  companion object {
    private const val DELIMITER = " "

    fun <T> insertBetween(vararg elements: T, separator: T): List<T> {
      val result = mutableListOf<T>()
      for ((index, element) in elements.withIndex()) {
        result.add(element)
        if (index != elements.lastIndex) {
          result.add(separator)
        }
      }
      return result
    }
  }
}