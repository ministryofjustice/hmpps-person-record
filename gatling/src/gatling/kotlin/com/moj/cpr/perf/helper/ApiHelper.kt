package com.moj.cpr.perf.helper

import com.moj.cpr.perf.config.AppConfig
import io.gatling.javaapi.core.ChainBuilder
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status

object ApiHelper {

    val getPrisoners: ChainBuilder =
        exec(
            http("GET Prisoner")
                .get(AppConfig.uriGetPrisoner)
                .header("Authorization", "Bearer ${TokenManager.getToken()}")
                .check(status().shouldBe(200))
        )

    val getCrns: ChainBuilder = exec(
        http("GET Crn")
            .get(AppConfig.uriGetCrn)
            .header("Authorization", "Bearer ${TokenManager.getToken()}")
            .check(status().shouldBe(200))
    )

    val getDefendants: ChainBuilder = exec(
        http("GET Defendant id")
            .get(AppConfig.uriGetDefendantId)
            .header("Authorization", "Bearer ${TokenManager.getToken()}")
            .check(status().shouldBe(200))
    )
}