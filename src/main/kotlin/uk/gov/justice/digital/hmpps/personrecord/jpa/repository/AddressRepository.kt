package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import java.util.UUID

@Repository
interface AddressRepository : JpaRepository<AddressEntity, Long> {

  fun findByUpdateId(updateId: UUID): AddressEntity?

  fun findByUpdateIdAndPersonCrn(updateId: UUID, personCrn: String): AddressEntity?
}
