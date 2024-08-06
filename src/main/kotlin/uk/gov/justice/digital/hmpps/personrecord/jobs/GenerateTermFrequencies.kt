package uk.gov.justice.digital.hmpps.personrecord.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.termfrequency.PncFrequencyRepository

@RestController
class GenerateTermFrequencies(
  private val pncFrequencyRepository: PncFrequencyRepository,
) {

  @RequestMapping(method = [RequestMethod.POST], value = ["/generate/termfrequencies"])
  suspend fun generate(): String {
    generatePncTermFrequencies()
    return OK
  }

  private suspend fun generatePncTermFrequencies() {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("Starting PNC term frequency generation")
      pncFrequencyRepository.deleteAll()
      pncFrequencyRepository.generatePncTermFrequency()
      log.info("Finished PNC term frequency generation")
    }
  }
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val OK = "OK"
  }
}
