package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

object SyncInsertHandlerHelper {

  fun <T> MutableList<T>.addIfAbsent(item: T): Boolean = item !in this && add(item)

  fun <T> MutableList<T>.addIfAbsent(items: Iterable<T>) = items.forEach { addIfAbsent(it) }
}
