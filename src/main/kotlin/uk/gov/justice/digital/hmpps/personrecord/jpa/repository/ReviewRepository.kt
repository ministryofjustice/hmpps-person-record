package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.review.ReviewEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.review.ClusterType

@Repository
interface ReviewRepository : JpaRepository<ReviewEntity, Long> {
  fun findByClustersClusterTypeAndClustersPersonKey(clusterType: ClusterType, personKeyEntity: PersonKeyEntity): ReviewEntity?
}
