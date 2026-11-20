package uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class CompareRecordsDetail(
  val records: List<AdminRecordDetail>,
  val clusterSpec: Any?,
) {
  companion object {
    fun from(personEntities: List<PersonEntity>, clusterSpec: Any?): CompareRecordsDetail = CompareRecordsDetail(
      records = personEntities.map { AdminRecordDetail.from(it) },
      clusterSpec = clusterSpec,
    )
  }
}
