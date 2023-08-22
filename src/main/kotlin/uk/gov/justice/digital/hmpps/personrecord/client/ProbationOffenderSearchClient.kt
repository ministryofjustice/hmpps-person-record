package uk.gov.justice.digital.hmpps.personrecord.client

import jakarta.validation.ValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RequestBody
import uk.gov.justice.digital.hmpps.personrecord.client.model.OffenderDetail
import uk.gov.justice.digital.hmpps.personrecord.client.model.SearchDto

// @FeignClient(
//  name = "offender-search",
//  url = "\${offender-search.base-url}",
//  configuration = [FeignOAuth2Config::class]
// )
@Component
class ProbationOffenderSearchClient(
  @Qualifier("offenderSearchApiWebClient")
  val webClient: AuthenticatingRestClient,
) {

  @Value("\${offender-search.base-url}")
  private lateinit var offenderSearchApiBaseUrl: String

  @Value("\${offender-search.offender-detail}")
  private lateinit var searchUrl: String

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun getOffenderDetail(@RequestBody searchDto: SearchDto): List<OffenderDetail>? {
    log.debug("Entered getOffenderDetail() with searchDto : $searchDto")

    val path = offenderSearchApiBaseUrl + searchUrl
    return webClient
      .post(path, searchDto)
      .retrieve()
      .onStatus(HttpStatusCode::is4xxClientError) {
        log.error("${it.statusCode().value()} Error retrieving offender details for: $searchDto")
        it.bodyToMono(ValidationException::class.java)
          .map { ValidationException("Error retrieving offender details") }
      }
      .bodyToMono(object : ParameterizedTypeReference<List<OffenderDetail>>() {})
      .block()
  }
}
