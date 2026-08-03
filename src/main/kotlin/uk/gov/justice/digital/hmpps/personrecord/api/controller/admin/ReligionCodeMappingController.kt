package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

@Hidden
@RestController
class ReligionCodeMappingController(
  private val personRepository: PersonRepository,
) {

  private val religionCodesByOldCodes = ReligionCode.entries.associateBy { it.description }

  private val alternativeCodesByNewCodes = mapOf(
    "Christian" to ReligionCode.CHRST,
    "Church of England (Anglican)" to ReligionCode.COFE,
    "Roman Catholic" to ReligionCode.RC,
    "No Religion" to ReligionCode.NIL,
  )

  @PutMapping("/admin/religion-code-mappings")
  @Transactional
  suspend fun mapOldReligionCodes() {
    CoroutineScope(Dispatchers.Default).launch {
      val personsWithOldReligionCodes = personRepository.findAllPersonsWithOldReligionCodes()
      personsWithOldReligionCodes.forEach { personEntity ->
        val religionCode = religionCodesByOldCodes[personEntity.religion] ?: alternativeCodesByNewCodes[personEntity.religion]
        if (religionCode != null) {
          personEntity.religion = religionCode.name
        }
      }
      personRepository.saveAll(personsWithOldReligionCodes)
    }
  }
}
