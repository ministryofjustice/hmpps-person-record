package uk.gov.justice.digital.hmpps.personrecord.seeding

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ProbationMergedRecordsResolverController(
  private val probationMergedRecordsResolver: ProbationMergedRecordsResolver,
) {

  @Hidden
  @PostMapping(value = ["/admin/resolve-merged-records"])
  fun populate(@RequestBody config: ResolveMergedRecordsConfig): String {
    probationMergedRecordsResolver.resolve(config)
    return "OK"
  }
}

data class ResolveMergedRecordsConfig(val crnToDelete: String, val crnToRecreate: String)
