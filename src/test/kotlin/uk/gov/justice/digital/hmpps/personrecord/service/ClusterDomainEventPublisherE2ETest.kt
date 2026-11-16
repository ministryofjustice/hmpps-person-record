package uk.gov.justice.digital.hmpps.personrecord.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.config.E2ETestBase

class ClusterDomainEventPublisherE2ETest : E2ETestBase() {

  @Nested
  inner class PersonJoinedCluster {

    @Test
    fun `should publish a CPR cluster changed domain event when a person has joined an existing cluster`() {
    }

    @Test
    fun `should not publish a CPR cluster changed domain event when a person joins a new cluster`() {
    }
  }

  @Nested
  inner class PersonLeftCluster {

    @Test
    fun `should publish a CPR cluster changed domain event when a person has left a multi person cluster`() {
    }

    @Test
    fun `should not publish a CPR cluster changed domain event when a single person cluster is deleted`() {
    }
  }
}
