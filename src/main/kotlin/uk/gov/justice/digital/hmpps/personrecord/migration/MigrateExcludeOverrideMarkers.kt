package uk.gov.justice.digital.hmpps.personrecord.migration

import io.swagger.v3.oas.annotations.Hidden
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.OverrideScopeEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.MarkerRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.OverrideScopeRepository
import uk.gov.justice.digital.hmpps.personrecord.jpa.repository.PersonRepository
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ActorType
import uk.gov.justice.digital.hmpps.personrecord.model.types.overridescopes.ConfidenceType
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService
import kotlin.time.Duration
import kotlin.time.measureTime

@RestController
class MigrateExcludeOverrideMarkers(
  private val markerRepository: MarkerRepository,
  private val personRepository: PersonRepository,
  private val personMatchService: PersonMatchService,
  private val overrideScopeRepository: OverrideScopeRepository,
) {

  @Hidden
  @RequestMapping(method = [RequestMethod.POST], value = ["/migrate/exclude-markers"])
  suspend fun migrate(): String {
    migrateExcludeMarkers()
    return OK
  }

  suspend fun migrateExcludeMarkers() = coroutineScope {
    log.info("Starting migration of exclude override markers")
    val excludedPeople = markerRepository.findDistinctPairsOfOverrideMarkers()
    val elapsedTime: Duration = measureTime {
      excludedPeople?.forEachIndexed { index, personPair ->
        log.info("Migrating exclude override markers, count: ${index + 1}")
        personPair?.let {
          val firstRecord = personRepository.findByIdOrNull(it.firstPersonId)
          val secondRecord = personRepository.findByIdOrNull(it.secondPersonId)
          handleExcludeRecords(firstRecord, secondRecord)
        }
      }
    }
    val executionResult = ExecutionResult(
      totalElements = excludedPeople?.size ?: 0,
      elapsedTime = elapsedTime,
    )
    log.info(
      "Finished migrating exclude override markers," +
        "total elements: ${executionResult.totalElements}, " +
        "elapsed time: ${executionResult.elapsedTime}",
    )
  }

  private fun handleExcludeRecords(firstRecord: PersonEntity?, secondRecord: PersonEntity?) {
    when {
      recordsDontExist(firstRecord, secondRecord) -> log.info("Pair of records: not found, id: ${firstRecord?.id} and ${secondRecord?.id}")
      overrideScopeAndMarkerAlreadySet(firstRecord!!, secondRecord!!) -> log.info("Override already exists")
      else -> excludeRecords(firstRecord, secondRecord)
    }
  }

  private fun excludeRecords(firstRecord: PersonEntity, secondRecord: PersonEntity) {
    val scopeEntity: OverrideScopeEntity = overrideScopeRepository.save(
      OverrideScopeEntity.new(confidence = ConfidenceType.VERIFIED, actor = ActorType.SYSTEM),
    )
    firstRecord.addOverrideMarker(scopeEntity)
    secondRecord.addOverrideMarker(scopeEntity)
    personRepository.saveAll(listOf(firstRecord, secondRecord))
    personMatchService.saveToPersonMatch(firstRecord)
    personMatchService.saveToPersonMatch(secondRecord)
  }

  private fun recordsDontExist(firstRecord: PersonEntity?, secondRecord: PersonEntity?) = firstRecord == null || secondRecord == null

  private fun overrideScopeAndMarkerAlreadySet(firstRecord: PersonEntity, secondRecord: PersonEntity): Boolean = firstRecord.overrideMarker != secondRecord.overrideMarker &&
    firstRecord.overrideScopes.map { it.scope }.toSet().intersect(
      secondRecord.overrideScopes.map { it.scope }.toSet(),
    ).isNotEmpty()

  private data class ExecutionResult(
    val totalElements: Int,
    val elapsedTime: Duration,
  )

  companion object {
    private const val OK = "OK"
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
