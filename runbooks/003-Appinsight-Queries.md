# 003 - AppInsights Queries

This is a runbook for queries in AppInsights to extract information about the system / record.

It will provide queries to execute within AppInsights.

## Prerequisites
* Must have an Azure account with the correct levels of AzureAD access to run in the required environment e.g. DEV, PREPROD and PROD

## 1. Multiple High-Confidence UUID results

This query returns the number of UUID's found when searching for a UUID to link to. 
With the associated searching record system identifier e.g. CRN, DEFENDANT_ID, PRISON_NUMBER and the source system from with the record originated. 

Run this query:
```
let multipleUuidMatches = AppEvents
| where AppRoleName == ('hmpps-person-record')
| where Name == "CprCandidateRecordSearch"
| where Properties.QUERY == "FIND_CANDIDATES_WITH_UUID"
| extend numOfUuids = toint(Properties.HIGH_CONFIDENCE_COUNT)
| where numOfUuids > 1
| project numOfUuids, OperationId;

let foundUuid = AppEvents
| where AppRoleName == ('hmpps-person-record')
| where Name == "CprSplinkCandidateRecordsFoundGetUUID"
| extend DefendantId = Properties.DEFENDANT_ID, CRN = Properties.CRN, PrisonNumber = Properties.PRISON_NUMBER, SourceSystem = Properties.SOURCE_SYSTEM
| project DefendantId, CRN, PrisonNumber, SourceSystem, OperationId;

multipleUuidMatches
| join foundUuid on $left.OperationId == $right.OperationId
| project numOfUuids, DefendantId, CRN, PrisonNumber, SourceSystem
| order by numOfUuids
```

Will output in the format (with examples):

| numOfUuids | DefendantId                          | CRN | PrisonNumber | SourceSystem    |
|------------|--------------------------------------|-----|--------------|-----------------|
| 7          | 66688f70-79fb-4fbf-acb7-3e2924b781b9 | ... | ...          | COMMON_PLATFORM |
| ...        | ...                                  | ... | ...          |                 |
