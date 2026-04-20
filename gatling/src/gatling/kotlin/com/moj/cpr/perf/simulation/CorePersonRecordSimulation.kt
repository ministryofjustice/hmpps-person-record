package com.moj.cpr.perf.simulation

import com.moj.cpr.perf.config.AppConfig
import com.moj.cpr.perf.helper.ApiHelper
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.listFeeder
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.PopulationBuilder
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import java.time.Duration

class CorePersonRecordSimulation : Simulation() {

  val allData = csv("testdata/data.csv").readRecords()
  private var prisonNumber = listFeeder(allData.map { mapOf("prison_number" to it["prison_number"]) }).circular()
  private var crn = listFeeder(allData.map { mapOf("crn" to it["crn"]) }).circular()
  private var defendantId = listFeeder(allData.map { mapOf("defendant_id" to it["defendant_id"]) }).circular()

  private val httpProtocol = http.baseUrl(AppConfig.baseUrl)
    .acceptHeader("application/json").shareConnections()


  private val scnPrisonNumber =
    scenario("prisonNumber")
      .feed(prisonNumber)
      .exec(ApiHelper.getPrisoners)

  private val scnCrn =
    scenario("crn")
      .feed(crn)
      .exec(ApiHelper.getCrns)

  private val scnDefendantId =
    scenario("defendantId")
      .feed(defendantId)
      .exec(ApiHelper.getDefendants)

  init {
    val populations = mutableListOf<PopulationBuilder>()
    populations.add(
      scnPrisonNumber.injectOpen(
        constantUsersPerSec(AppConfig.getPrisonNumberUsers.toDouble()).during(
          AppConfig.duration,
        ).randomized(),
      ),
    )
    populations.add(
      scnCrn.injectOpen(
        constantUsersPerSec(AppConfig.getCrnUsers.toDouble()).during(AppConfig.duration).randomized(),
      ),
    )
    populations.add(
      scnDefendantId.injectOpen(
        constantUsersPerSec(AppConfig.getDefendantIdUsers.toDouble()).during(
          AppConfig.duration,
        ).randomized(),
      ),
    )
    setUp(*populations.toTypedArray())
      .protocols(httpProtocol)
      .maxDuration(Duration.ofSeconds(AppConfig.duration))
  }
}