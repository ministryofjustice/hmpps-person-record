package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

const val PERSON_RECORD_SERVICE = "PERSON-RECORD-SERVICE"

@EntityListeners(AuditingEntityListener::class)
@MappedSuperclass
open class BaseAuditedEntity {

  @Column(name = "created_date", nullable = false, updatable = false)
  @CreatedDate
  var createdDate: LocalDateTime? = LocalDateTime.now()

  @Column(name = "created_by", nullable = true, updatable = false)
  @CreatedBy
  var createdBy: String? = null

  @Column(name = "last_updated_date", nullable = false)
  @LastModifiedDate
  var lastUpdatedDate: LocalDateTime? = LocalDateTime.now()

  @Column(name = "last_updated_by")
  @LastModifiedBy
  var lastUpdatedBy: String? = null

  @Version
  var version: Int = 0
}
