package uk.gov.justice.digital.hmpps.personrecord.jpa.repository

import org.springframework.data.jpa.repository.JpaRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.SentenceInfoEntity

interface SentenceInfoRepository : JpaRepository<SentenceInfoEntity, Long>
