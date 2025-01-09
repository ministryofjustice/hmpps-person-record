package uk.gov.justice.digital.hmpps.personrecord.client.model.prisoner

data class CurrentlyManaged(val status: Boolean?) {

  companion object {
    private const val EMPTY_STATUS = ""

    private enum class Status(val status: String) {
      ACTIVE_IN("ACTIVE IN"),
      ACTIVE_OUT("ACTIVE OUT"),
      INACTIVE_TRANSFER("INACTIVE TRN"),
      INACTIVE_OUT("INACTIVE OUT"),
    }

    fun from(inputStatus: String? = EMPTY_STATUS): CurrentlyManaged {
      val status: Boolean? = when (inputStatus) {
        Status.ACTIVE_IN.status -> true
        Status.ACTIVE_OUT.status -> true
        Status.INACTIVE_TRANSFER.status -> true
        Status.INACTIVE_OUT.status -> false
        else -> null
      }
      return CurrentlyManaged(status)
    }
  }
}
