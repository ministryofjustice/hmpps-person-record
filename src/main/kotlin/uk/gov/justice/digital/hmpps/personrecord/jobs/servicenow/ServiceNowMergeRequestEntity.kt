package uk.gov.justice.digital.hmpps.personrecord.jobs.servicenow

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import java.util.UUID

@Entity
@Table(name = "service_now_probation_merge_request")
class ServiceNowMergeRequestEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @Column(name = "person_uuid")
  val personUUID: UUID? = null,
) {
  companion object {
    fun from(it: PersonEntity): ServiceNowMergeRequestEntity = ServiceNowMergeRequestEntity(personUUID = it.personKey?.personUUID)
    fun fromUuid(uuid: UUID?): ServiceNowMergeRequestEntity = ServiceNowMergeRequestEntity(personUUID = uuid)
  }
}
