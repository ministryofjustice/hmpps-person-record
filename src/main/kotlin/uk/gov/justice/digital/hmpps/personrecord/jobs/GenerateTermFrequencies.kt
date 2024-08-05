package uk.gov.justice.digital.hmpps.personrecord.jobs

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.termfrequency.PncFrequencyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PncFrequencyRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType
import uk.gov.justice.digital.hmpps.personrecord.service.TermFrequencyService

@RestController
class GenerateTermFrequencies(
  private val termFrequencyService: TermFrequencyService,
  private val pncFrequencyRepository: PncFrequencyRepository,
) {

  @RequestMapping(method = [RequestMethod.POST], value = ["/generatetermfrequencies"])
  fun generate(): String {
    generatePncTermFrequencies()
    return OK
  }

  private fun generatePncTermFrequencies() {
    log.info("Starting PNC term frequency generation")
    val pncTermFrequencies = termFrequencyService.findReferenceTermFrequencies(IdentifierType.PNC)
    val pncFrequencyEntities = pncTermFrequencies.map { PncFrequencyEntity.from(it) }
    pncFrequencyRepository.saveAllAndFlush(pncFrequencyEntities)
    log.info("Finished PNC term frequency generation")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val OK = "OK"
  }
}
