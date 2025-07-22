package uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity

data class AdminClusterDetail(
  val uuid: String,
  val status: String,
  val records: List<AdminRecordDetail>,
) {
  companion object {
    fun from(personKeyEntity: PersonKeyEntity): AdminClusterDetail = AdminClusterDetail(
      uuid = personKeyEntity.personUUID.toString(),
      status = personKeyEntity.status.name,
      records = personKeyEntity.personEntities.map { AdminRecordDetail.from(it) },
    )
  }
}
