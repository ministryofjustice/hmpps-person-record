package uk.gov.justice.digital.hmpps.personrecord.jobs

interface BatchJob {
  val jobName: String
  fun run()
}
