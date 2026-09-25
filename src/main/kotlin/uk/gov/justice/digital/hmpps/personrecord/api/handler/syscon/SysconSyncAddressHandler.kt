package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAddress

@Component
class SysconSyncAddressHandler {

  @Transactional
  fun handleInsert(
    prisonNumber: String,
    prisonAddress: PrisonAddress
  ){

  }
}