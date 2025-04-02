package uk.gov.justice.digital.hmpps.personrecord.client.model.match.isclustervalid

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class IsClusterValidRequest(
  val matchIds: List<String>,
) {
  companion object {
    fun from(cluster: PersonKeyEntity): IsClusterValidRequest = IsClusterValidRequest(matchIds = cluster.personEntities.map { it.matchId.toString() })
  }
}
