package uk.gov.justice.digital.hmpps.personrecord.model

enum class MessageType {
  LIBRA_COURT_CASE,
  COMMON_PLATFORM_HEARING,
  UNKNOWN
}

fun of(messageType: String): MessageType {
  return when (messageType) {
    "LIBRA_COURT_CASE" -> MessageType.LIBRA_COURT_CASE
    "COMMON_PLATFORM_HEARING" -> MessageType.COMMON_PLATFORM_HEARING
    else -> MessageType.UNKNOWN
  }
}
