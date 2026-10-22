package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class PrisonerImageClient(
  private val prisonWebClient: WebClient,
) {
  fun getPrisonerImage(
    offenderNo: String,
    fullSizeImage: Boolean,
  ): ByteArray = prisonWebClient
    .get()
    .uri { builder ->
      builder
        .path("/api/bookings/offenderNo/{offenderNo}/image/data")
        .queryParam("fullSizeImage", fullSizeImage)
        .build(offenderNo)
    }
    .accept(MediaType.IMAGE_JPEG)
    .retrieve()
    .bodyToMono<ByteArray>()
    .block()
    ?.takeIf { it.isNotEmpty() }
    ?: throw IllegalStateException(
      "Image source returned an empty response body",
    )
}
