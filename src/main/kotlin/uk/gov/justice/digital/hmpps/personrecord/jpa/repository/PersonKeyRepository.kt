package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.UUIDStatusType
import java.util.*

@Repository
interface PersonKeyRepository : JpaRepository<PersonKeyEntity, Long> {
  fun findByPersonUUID(personUUID: UUID?): PersonKeyEntity?

  fun findAllByStatusOrderById(uuidStatus: UUIDStatusType, pageable: Pageable): Page<PersonKeyEntity>
}
