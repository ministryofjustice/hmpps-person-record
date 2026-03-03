package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.controller.CanonicalAggregationEngine
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonKeyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import java.net.URI
import java.util.UUID

@Hidden
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
class CanonicalAggreationApiController(
  private val personKeyRepository: PersonKeyRepository,
  private val canonicalAggregationEngine: CanonicalAggregationEngine,
) {

  @GetMapping("/canonical-record/{uuid}")
  fun getCanonicalRecordSpike(
    @PathVariable(name = "uuid") uuid: UUID,
  ): ResponseEntity<*> {
    val personKeyEntity = getCorrectPersonKeyEntity(personKeyRepository.findByPersonUUID(uuid), mutableSetOf())
    return when {
      personKeyEntity == null -> throw ResourceNotFoundException(uuid.toString())
      personKeyEntity.isNotRequestedUuid(uuid) -> ResponseEntity.status(HttpStatus.MOVED_PERMANENTLY).location(URI("/person/${personKeyEntity.personUUID}")).build<Void>()
      else -> {
        val result = canonicalAggregationEngine.get(personKeyEntity)
        ResponseEntity.ok(result)
      }
    }
  }

  private fun getCorrectPersonKeyEntity(personKeyEntity: PersonKeyEntity?, existingMergeChain: MutableSet<UUID?>): PersonKeyEntity? = personKeyEntity?.mergedTo?.let {
    existingMergeChain.add(personKeyEntity.personUUID)
    getCorrectPersonKeyEntity(personKeyRepository.findByIdOrNull(it), existingMergeChain)
  }
    ?: personKeyEntity

  private fun PersonKeyEntity.isNotRequestedUuid(uuid: UUID): Boolean = this.personUUID != uuid
}
