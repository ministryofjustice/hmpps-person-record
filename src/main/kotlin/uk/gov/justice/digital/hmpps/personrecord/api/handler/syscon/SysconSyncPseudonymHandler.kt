package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping

interface SysconSyncPseudonymHandlerProxy {
  fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias): SysconAliasMapping
}

@Profile("prod || preprod")
@Component
class SysconSyncPseudonymHandlerProd : SysconSyncPseudonymHandlerProxy {
  override fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias) = SysconAliasMapping()
}

@Profile("!prod && !preprod")
@Component
class SysconSyncPseudonymHandlerImpl : SysconSyncPseudonymHandlerProxy {
  override fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias) = SysconAliasMapping()
}
