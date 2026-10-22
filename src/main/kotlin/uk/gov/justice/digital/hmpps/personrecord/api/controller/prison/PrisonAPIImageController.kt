package uk.gov.justice.digital.hmpps.personrecord.api.controller.prison

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.personrecord.api.constants.Roles.API_READ_ONLY
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonImageClient

@Tag(name = "Prison")
@RestController
@PreAuthorize("hasRole('$API_READ_ONLY')")
@RequestMapping("/person/prison")
class PrisonAPIImageController(
  private val prisonImageClient: PrisonImageClient,
) {

  @Operation(
    security = [SecurityRequirement(name = "api-role")],
    description = "Retrieve image data (as bytes) by prison number from Prison API." +
      "This is a proxy endpoint to prison api. Role required is **$API_READ_ONLY**.",
  )
  @GetMapping("/{prisonNumber}/image", produces = [MediaType.IMAGE_JPEG_VALUE])
  @ApiResponses(
    ApiResponse(
      responseCode = "200",
      description = "OK",
      content = [
        Content(
          mediaType = MediaType.IMAGE_JPEG_VALUE,
          schema = Schema(type = "string", format = "binary"),
        ),
      ],
    ),
  )
  fun getMainBookingImageDataByNo(
    @PathVariable @Parameter(example = "A1234AA") prisonNumber: String,
    @RequestParam(name = "fullSizeImage", required = false, defaultValue = "false") fullSizeImage: Boolean,
  ): ResponseEntity<ByteArray> = ResponseEntity.ok()
    .contentType(MediaType.IMAGE_JPEG).body(prisonImageClient.getPrisonerImage(prisonNumber, fullSizeImage))
}
