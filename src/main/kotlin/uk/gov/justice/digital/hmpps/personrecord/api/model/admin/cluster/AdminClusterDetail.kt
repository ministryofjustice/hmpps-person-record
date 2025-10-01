package uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusReasonType

data class AdminClusterDetail(
  val uuid: String,
  val status: String,
  val statusReason: AdminStatusReason?,
  val records: List<AdminRecordDetail>,
  val clusterSpec: Any,
) {
  companion object {
    fun from(personKeyEntity: PersonKeyEntity, clusterSpec: Any): AdminClusterDetail = AdminClusterDetail(
      uuid = personKeyEntity.personUUID.toString(),
      status = personKeyEntity.status.name,
      statusReason = personKeyEntity.statusReason?.let { AdminStatusReason.from(it) },
      records = personKeyEntity.personEntities.map { AdminRecordDetail.from(it) },
      clusterSpec = clusterSpec,
    )
  }
}

data class AdminStatusReason(
  val code: String,
  val description: String,
) {
  companion object {
    fun from(uuidStatusReasonType: UUIDStatusReasonType): AdminStatusReason = AdminStatusReason(
      code = uuidStatusReasonType.name,
      description = uuidStatusReasonType.description,
    )
  }
}
