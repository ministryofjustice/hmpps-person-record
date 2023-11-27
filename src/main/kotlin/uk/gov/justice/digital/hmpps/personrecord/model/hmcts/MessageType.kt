package uk.gov.justice.digital.hmpps.personrecord.model.hmcts

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

enum class MessageType {
  LIBRA_COURT_CASE,
  COMMON_PLATFORM_HEARING,
  UNKNOWN;

  companion object {
        /*About @jvmstatic: creators have to be static as they're used to create an instance of the object, that doesn't work well
          if you need an instance of the object to access them. So without @jvmstatic it's just ignored and Jackson falls back on
          default enum deserializer.*/
    @JsonCreator
    @JvmStatic
    fun of(@JsonProperty("Value") value: String?): MessageType {
      return when (value) {
        "LIBRA_COURT_CASE" -> LIBRA_COURT_CASE
        "COMMON_PLATFORM_HEARING" -> COMMON_PLATFORM_HEARING
        else -> UNKNOWN
      }
    }
  }
}
