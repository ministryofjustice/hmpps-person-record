package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import java.util.UUID

@Component
class ReclusterTwinsService(
  private val personKeyRepository: PersonKeyRepository,
  private val personRepository: PersonRepository,
) {
  @Transactional
  fun split(clusterToSplit: List<String>) {
    val personKey = PersonKeyEntity.new()
    clusterToSplit.forEach {
      val personEntity = personRepository.findByMatchId(UUID.fromString(it))!!
      personEntity.assignToPersonKey(personKey)
      personKeyRepository.save(personKey)
    }
    log.info("Created cluster ${personKey.personUUID} with these matchIds: ${personKey.personEntities.map { it.matchId }}")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
