package uk.gov.justice.digital.hmpps.personrecord.service.matcher

import uk.gov.justice.digital.hmpps.personrecord.model.Person

abstract class Matcher<T>(val items: List<T>?, protected val person: Person) {
  abstract fun isMatchingItem(item: T): Boolean
  abstract fun isPartialMatchItem(item: T): Boolean

  fun isExactMatch() = !items.isNullOrEmpty() && items.size == 1 && items.any(::isMatchingItem)
  fun isPartialMatch() = !items.isNullOrEmpty() && items.size == 1 && items.any(::isPartialMatchItem)
  fun isMultipleMatch() = !items.isNullOrEmpty() && items.size > 1 && items.all(::isMatchingItem)
  fun getAllMatchingItems() = items?.filter(::isMatchingItem)
  fun getItemDetail(): T = items!!.first()
}
