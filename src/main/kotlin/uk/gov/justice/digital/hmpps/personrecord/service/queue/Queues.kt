package uk.gov.justice.digital.hmpps.personrecord.service.queue

object Queues {
  // CPR
  const val RECLUSTER_EVENTS_QUEUE_ID = "cprreclustereventsqueue"

  // COURTS
  const val COURT_CASES_QUEUE_ID = "cprcourtcasesqueue"

  // PRISON
  const val PRISON_EVENT_QUEUE_ID = "cprnomiseventsqueue"
  const val PRISON_MERGE_EVENT_QUEUE_ID = "cprnomismergeeventsqueue"

  // PROBATION
  const val PROBATION_DELETION_EVENT_QUEUE_ID = "cprdeliusdeleteeventsqueue"
  const val PROBATION_EVENT_QUEUE_ID = "cprdeliusoffendereventsqueue"
  const val PROBATION_MERGE_EVENT_QUEUE_ID = "cprdeliusmergeeventsqueue"
}
