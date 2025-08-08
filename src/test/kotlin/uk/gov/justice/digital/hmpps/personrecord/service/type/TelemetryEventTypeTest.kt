package uk.gov.justice.digital.hmpps.personrecord.service.type

import org.junit.jupiter.api.Test

class TelemetryEventTypeTest {

  // Run this query, export to csv and copy into liveEvents
  // https://portal.azure.com#@747381f4-e81f-4a43-bf68-ced6a1e14edf/blade/Microsoft_OperationsManagementSuite_Workspace/Logs.ReactView/resourceId/%2Fsubscriptions%2Fa5ddf257-3b21-4ba9-a28c-ab30f751b383%2FresourceGroups%2Fnomisapi-prod-rg%2Fproviders%2FMicrosoft.OperationalInsights%2Fworkspaces%2Fnomisapi-prod/source/LogsBlade.AnalyticsShareLinkToQuery/q/H4sIAAAAAAAAAzWLIQ6AMBAEPa84BwiegKjAIvhBKZtAQtvLXQuB8HiKQE5mxjAPB0LS6qFzhYAM8xR3jNaD%252Bp6aevXM2jFEY%252BgELspStyXX7L2V7Qa5mENqWpov%252BrbiSgP5may6F6x538FoAAAA/timespan/2024-08-26T08%3A18%3A41.000Z%2F2025-02-26T08%3A18%3A41.107Z/limit/30000
  val liveEvents = """
CprCandidateRecordSearch,6508689
CprDefendantEventReceived,6748678
CprDomainEventReceived,15432404
CprFIFOHearingCreated,331320
CprFIFOHearingUpdated,922278
CprMatchCallFailed,3787
CprMatchPersonRecordDuplicate,150846
CprMatchScore,58730100
CprMergeEventReceived,6342
CprMergeRecordNotFound,179
CprMessageProcessingFailed,50877
CprNewRecordExists,14112
CprReclusterClusterRecordsNotLinked,345660
CprReclusterMatchFoundMergeRecluster,25614
CprReclusterMessageReceived,7242924
CprReclusterNoChange,930595
CprReclusterNoMatchFound,2790260
CprRecordCreated,1979449
CprRecordMerged,1715
CprRecordUnmerged,7
CprRecordUpdated,13908252
CprSplinkCandidateRecordsFoundGetUUID,135546
CprSplinkSelfMatch,8132349
CprSplinkSelfMatchNotCreatingUuid,67297
CprUnmergeEventReceived,112
CprUnmergeLinkNotFound,3
CprUpdateRecordDoesNotExist,61207
CprUuidCreated,1798999
CprUuidReclusterNeedsAttention,3144128
RetryDLQ,3228
"""

  @Test
  fun `output telemetry event types`() {
    val codeEvents = TelemetryEventType.entries.map { it.eventName }.sorted()

    val mappedLiveEvents = liveEvents
      .split("\n")
      .filterNot { it.isBlank() }
      .associate { it.split(",")[0] to it.split(",")[1] }

    codeEvents.filterNot { mappedLiveEvents.containsKey(it) }.forEach { println(it) }
  }
}
