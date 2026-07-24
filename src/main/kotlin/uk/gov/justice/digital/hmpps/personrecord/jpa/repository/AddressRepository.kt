package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import java.util.UUID

@Repository
interface AddressRepository : JpaRepository<AddressEntity, Long> {

  fun findByUpdateId(updateId: UUID): AddressEntity?

  fun findByUpdateIdAndPersonCrn(updateId: UUID, personCrn: String): AddressEntity?

  fun findByDeliusAddressId(id: Long?): AddressEntity?

  @Query("select a.deliusAddressId from AddressEntity a join a.usages u where u.usageCode = 'UNKNOWN' order by a.id desc")
  fun findDeliusAddressIdByUnknownUsageCode(pageable: Pageable): Page<Long>
}
