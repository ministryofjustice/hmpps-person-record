package uk.gov.justice.digital.hmpps.personrecord.api.handler.syscon

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.PrisonAlias
import uk.gov.justice.digital.hmpps.personrecord.api.model.sysconsync.response.SysconAliasMapping

interface SysconSyncPseudonymHandler {
  fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias): SysconAliasMapping
}

@Profile("dev || prod || preprod")
@Component
class SysconSyncPseudonymHandlerProd : SysconSyncPseudonymHandler {
  override fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias) = SysconAliasMapping()
}

@Profile("!dev && !prod && !preprod")
@Component
class SysconSyncPseudonymHandlerImpl : SysconSyncPseudonymHandler {
  override fun handleInsert(prisonNumber: String, prisonAlias: PrisonAlias) = SysconAliasMapping()
}
