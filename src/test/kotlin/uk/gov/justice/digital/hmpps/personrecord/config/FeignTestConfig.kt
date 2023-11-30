package uk.gov.justice.digital.hmpps.personrecord.config

import com.fasterxml.jackson.databind.JsonNode
import feign.RequestInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.web.client.RestTemplate

@Configuration
class FeignTestConfig {

  @Bean
  @Profile("test")
  fun requestInterceptorForTest() = RequestInterceptor { template ->
    template.header(HttpHeaders.AUTHORIZATION, "Bearer ${getLocalAccessToken()}")
  }

  private fun getLocalAccessToken(): String {
    val authResponse = RestTemplate()
      .postForObject("http://localhost:8090/auth/oauth/token", null, JsonNode::class.java)!!
    return authResponse["access_token"].asText()
  }
}
