package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.termfrequency.PncFrequencyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PncFrequencyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository

@RestController
class GenerateTermFrequencies(
  val referenceRepository: ReferenceRepository,
  val pncFrequencyRepository: PncFrequencyRepository,
) {

  @RequestMapping(method = [RequestMethod.POST], value = ["/generate/termfrequencies"])
  fun generate(): String {
    generatePncTermFrequencies()
    return OK
  }

  private fun generatePncTermFrequencies() {
    log.info("Starting PNC term frequency generation")
    val pncTermFrequencies = referenceRepository.getTermFrequencyForPnc()
    val pncFrequencyEntities = pncTermFrequencies.map { PncFrequencyEntity.from(it) }
    pncFrequencyRepository.saveAllAndFlush(pncFrequencyEntities)
    log.info("Finished PNC term frequency generation")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val OK = "OK"
  }
}
