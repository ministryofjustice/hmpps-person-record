package uk.gov.justice.digital.hmpps.personrecord.client

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Flux

@Component
class PrisonImageClient(
  private val prisonWebClient: WebClient,
) {
  fun getPrisonerImage(
    offenderNo: String,
    fullSizeImage: Boolean,
  ): Flux<DataBuffer> = prisonWebClient
    .get()
    .uri { builder ->
      builder
        .path("/api/bookings/offenderNo/{offenderNo}/image/data")
        .queryParam("fullSizeImage", fullSizeImage)
        .build(offenderNo)
    }
    .accept(MediaType.IMAGE_JPEG, MediaType.APPLICATION_JSON)
    .retrieve()
    .bodyToFlux<DataBuffer>()
    .switchIfEmpty(
      Flux.error(IllegalStateException("Image source returned an empty response body")),
    )
}
