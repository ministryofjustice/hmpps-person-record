package uk.gov.justice.digital.hmpps.personrecord.api.controller.canonical

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.PERSON_RECORD_ADMIN_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions.ResourceNotFoundException
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonKeyRepository
import java.util.UUID

@Hidden
@RestController
@PreAuthorize("hasRole('$PERSON_RECORD_ADMIN_READ_ONLY')")
class CanonicalAggregationApiController(
  private val personKeyRepository: PersonKeyRepository,
  private val canonicalAggregationEngine: CanonicalAggregationEngine,
) {

  @GetMapping("/canonical-record/{uuid}")
  fun getCanonicalRecord(
    @PathVariable(name = "uuid") uuid: UUID,
  ): ResponseEntity<*> {
    val personKeyEntity = personKeyRepository.findByPersonUUID(uuid)
    return when {
      personKeyEntity == null -> throw ResourceNotFoundException(uuid.toString())
      else -> ResponseEntity.ok(canonicalAggregationEngine.get(personKeyEntity))
    }
  }
}
