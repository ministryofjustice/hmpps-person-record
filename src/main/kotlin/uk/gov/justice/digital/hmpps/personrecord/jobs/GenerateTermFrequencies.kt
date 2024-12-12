package uk.gov.justice.digital.hmpps.personrecord.jobs

import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.termfrequency.PncFrequencyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.termfrequency.TermCleaningRepository

@RestController
@Tag(name = "Jobs")
class GenerateTermFrequencies(
  private val pncFrequencyRepository: PncFrequencyRepository,
  private val termCleaningRepository: TermCleaningRepository,
) {

  @RequestMapping(method = [RequestMethod.POST], value = ["/jobs/generatetermfrequencies"])
  suspend fun generate(): String {
    generatePncTermFrequencies()
    generatePersonCleanedData()
    return OK
  }

  private suspend fun generatePncTermFrequencies() {
    CoroutineScope(Dispatchers.Default).launch {
      log.info("Starting PNC term frequency generation")
      pncFrequencyRepository.deleteAllInBatch()
      pncFrequencyRepository.generatePncTermFrequency()
      log.info("Finished PNC term frequency generation")
    }
  }

  private suspend fun generatePersonCleanedData() {
    CoroutineScope(Dispatchers.Default).launch {
      collectPersonAggregateData()
      // Cleaned data depends upon aggregate data
      collectPersonCleanedData()
    }
  }

  private fun collectPersonAggregateData() {
    log.info("Starting collection of person aggregate data")
    termCleaningRepository.refreshPersonAggregateData()
    log.info("Finished collecting person aggregate data")
  }

  private fun collectPersonCleanedData() {
    log.info("Started collecting cleaned person data")
    termCleaningRepository.refreshPersonCleanedData()
    log.info("Finished collecting cleaned person data")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val OK = "OK"
  }
}
