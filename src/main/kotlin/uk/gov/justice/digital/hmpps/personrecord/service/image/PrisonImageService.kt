package uk.gov.justice.digital.hmpps.personrecord.service.image

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonImageClient

@Component
class PrisonImageService(
  private val prisonImageClient: PrisonImageClient,
) {

  fun getOffenderImage(offenderNo: String, fullSizeImage: Boolean): ByteArray = prisonImageClient.getPrisonerImage(offenderNo, fullSizeImage)
}
