package uk.gov.justice.digital.hmpps.personrecord.service

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import jakarta.persistence.criteria.Subquery
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.personrecord.client.model.termfrequency.TermFrequency
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ReferenceEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

@Service
class TermFrequencyService(
  private val entityManager: EntityManager
) {

  fun findReferenceTermFrequencies(identifierType: IdentifierType): List<TermFrequency> {
    val cb: CriteriaBuilder = entityManager.criteriaBuilder
    val cq: CriteriaQuery<TermFrequency> = cb.createQuery(TermFrequency::class.java)
    val root: Root<ReferenceEntity> = cq.from(ReferenceEntity::class.java)

    // Subquery for the total count
    val subquery = calculateReferenceFrequencySubQuery(cb, cq, identifierType)

    // Main query
    cq.select(
      cb.construct(
        TermFrequency::class.java,
        root.get<String>(IDENTIFIER_VALUE),
        cb.quot(cb.count(root).`as`(Double::class.java), subquery)
      )
    )
      .where(
        cb.equal(root.get<String>(IDENTIFIER_TYPE), identifierType.name),
        cb.isNotNull(root.get<String>(IDENTIFIER_VALUE))
      )
      .groupBy(root.get<String>(IDENTIFIER_VALUE))

    // Execute the query
    val query = entityManager.createQuery(cq)
    return query.resultList
  }

  private fun calculateReferenceFrequencySubQuery(
    criteriaBuilder: CriteriaBuilder,
    criteriaQuery: CriteriaQuery<TermFrequency>,
    identifierType: IdentifierType,
  ): Subquery<Long> {
    val subquery: Subquery<Long> = criteriaQuery.subquery(Long::class.java)
    val subRoot: Root<ReferenceEntity> = subquery.from(ReferenceEntity::class.java)
    subquery.select(criteriaBuilder.count(subRoot.get<String>(IDENTIFIER_VALUE)))
      .where(
        criteriaBuilder.equal(subRoot.get<String>(IDENTIFIER_TYPE), identifierType.name),
        criteriaBuilder.isNotNull(subRoot.get<String>(IDENTIFIER_VALUE))
      )
    return subquery
  }

  companion object {
    private const val IDENTIFIER_TYPE = "identifierType"
    private const val IDENTIFIER_VALUE = "identifierValue"
  }
}