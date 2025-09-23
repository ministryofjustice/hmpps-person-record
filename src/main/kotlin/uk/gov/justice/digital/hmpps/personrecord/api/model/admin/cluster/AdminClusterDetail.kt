package uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class AdminClusterDetail(
  val uuid: String,
  val status: String,
  val records: List<AdminRecordDetail>,
  val clusterSpec: Any,
) {
  companion object {
    fun from(personKeyEntity: PersonKeyEntity, clusterSpec: Any): AdminClusterDetail = AdminClusterDetail(
      uuid = personKeyEntity.personUUID.toString(),
      status = personKeyEntity.status.name,
      records = personKeyEntity.personEntities.map { AdminRecordDetail.from(it) },
      clusterSpec = clusterSpec,
    )
  }
}
