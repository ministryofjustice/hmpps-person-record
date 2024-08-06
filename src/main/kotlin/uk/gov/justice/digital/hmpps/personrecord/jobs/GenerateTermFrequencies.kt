package uk.gov.justice.digital.hmpps.personrecord.jobs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.client.model.termfrequency.TermFrequency
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.termfrequency.PncFrequencyEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PncFrequencyRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.ReferenceRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.IdentifierType

@RestController
class GenerateTermFrequencies(
  private val pncFrequencyRepository: PncFrequencyRepository,
  private val referenceRepository: ReferenceRepository,
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
      val totalElements = forPage { page ->
        val pncFrequencyEntities = page.map { PncFrequencyEntity.from(it) }
        pncFrequencyRepository.saveAllAndFlush(pncFrequencyEntities)
      }
      log.info("Finished PNC term frequency generation, Total PNC's: $totalElements")
    }
  }

  private inline fun forPage(page: (Page<TermFrequency>) -> Unit): Long {
    var pageNum = 0
    var currentPage: Page<TermFrequency>
    do {
      currentPage = executePagedQuery(pageNum)
      if (currentPage.hasContent()) {
        page(currentPage)
      }
      pageNum++
    } while (currentPage.hasNext())
    return currentPage.totalElements
  }

  private fun executePagedQuery(pageNum: Int): Page<TermFrequency> =
    referenceRepository.getIdentifierTermFrequency(IdentifierType.PNC.name, PageRequest.of(pageNum, PAGE_SIZE))

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    private const val OK = "OK"
    private const val PAGE_SIZE = 500
  }
}
