package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

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

  @Query(
    value = """
      select a.* from person p
      join address a on a.fk_person_id = p.id
      where p.source_system = 'COMMON_PLATFORM'
    """,
    nativeQuery = true,
  )
  fun findAllByCommonPlatform(): List<AddressEntity>
}
