package uk.gov.justice.digital.hmpps.personrecord.jobs.recluster

import uk.gov.justice.digital.hmpps.personrecord.jobs.BatchJob
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository

class ReclusterDatabaseJob(
  private val personRepository: PersonRepository,
) : BatchJob {
  override val jobName = "RECLUSTER_DATABASE"

  override fun run() {
  }
}
