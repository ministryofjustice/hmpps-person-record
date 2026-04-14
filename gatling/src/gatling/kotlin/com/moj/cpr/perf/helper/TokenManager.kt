package com.moj.cpr.perf.helper

import com.moj.cpr.perf.config.AppConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.Base64

object TokenManager {
    private var cacheToken: String = ""
    private var expiryTime: Long = 0
    private val client = HttpClient.newHttpClient()

    @Synchronized
    fun getToken(): String {
        val now = Instant.now().epochSecond
        if (cacheToken.isNotEmpty() || now >= expiryTime - 300) {
            fetchToken()
        }
        return cacheToken
    }

    private fun fetchToken() {
      if (AppConfig.clientId.isBlank() || AppConfig.clientSecret.isBlank()) {
        throw IllegalStateException("Client credentials not configured - clientId and clientSecret must be provided")
      }
      val basicAuth =
        Base64.getEncoder().encodeToString("${AppConfig.clientId.trim()}:${AppConfig.clientSecret.trim()}".toByteArray())

      val request = HttpRequest.newBuilder().uri(URI.create(AppConfig.tokenUrl))
            .header("Authorization", "Basic $basicAuth")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 200) {
            val body = response.body()
            cacheToken = body.substringAfter("access_token\":\"").substringBefore("\"")
            val expiresIn = body.substringAfter("expires_in\":").substringBefore(",").toLongOrNull() ?: 3600L
            expiryTime = Instant.now().epochSecond + expiresIn
        } else {
            throw RuntimeException("Token service error: ${response.statusCode()}")
        }
    }
}
