package uk.gov.justice.digital.hmpps.personrecord.client.model.court.commonplatform

import com.fasterxml.jackson.annotation.JsonInclude
import kotlin.reflect.full.memberProperties

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Address(
  val address1: String? = null,
  val address2: String? = null,
  val address3: String? = null,
  val address4: String? = null,
  val address5: String? = null,
  val postcode: String? = null,
) {

  fun allPropertiesOrNull(): Address? = this.takeIf {
    it.allNullOrBlank().not()
  }

  fun allNullOrBlank(): Boolean =
    this::class.memberProperties.all { prop ->
      when (val value = prop.getter.call(this)) {
        null -> true
        is String -> value.isBlank()
        else -> false
      }
    }
}
