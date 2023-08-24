package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.DeliusOffenderEntity

@Repository
interface DeliusOffenderRepository: JpaRepository<DeliusOffenderEntity, Long> {

  fun findByCrn(crn: String) : DeliusOffenderEntity ?

  fun existsByCrn(crn :String) : Boolean


}