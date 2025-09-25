package uk.gov.justice.digital.hmpps.personrecord.api.model.admin.cluster

import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity

data class AdminRecordDetail(
  val sourceSystemId: String?,
  val sourceSystem: String,
  val firstName: String?,
  val middleName: String?,
  val lastName: String?,
) {
  companion object {
    fun from(personEntity: PersonEntity): AdminRecordDetail {
      val primaryName = personEntity.getPrimaryName()
      return AdminRecordDetail(
        sourceSystemId = personEntity.extractSourceSystemId(),
        sourceSystem = personEntity.sourceSystem.name,
        firstName = primaryName.firstName,
        middleName = primaryName.middleNames,
        lastName = primaryName.lastName,
      )
    }
  }
}
