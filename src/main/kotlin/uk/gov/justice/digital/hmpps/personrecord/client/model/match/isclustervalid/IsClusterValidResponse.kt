package uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid

data class IsClusterValidResponse(
  val isClusterValid: Boolean,
  val clusters: List<List<String>>,
) {
  companion object {

    fun IsClusterValidResponse.result(isValid: () -> Unit, isNotValid: (clusters: List<ValidCluster>) -> Unit) {
      when {
        this.isClusterValid -> isValid()
        else -> isNotValid(this.clusters.map { cluster -> ValidCluster(cluster) })
      }
    }
  }
}
