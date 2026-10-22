package uk.gov.justice.digital.hmpps.personrecord.service.image

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.client.PrisonerImageClient

@Component
class PrisonImageService(
  private val prisonerImageClient: PrisonerImageClient,
) {

  fun getOffenderImage(offenderNo: String, fullSizeImage: Boolean): ByteArray = prisonerImageClient.getPrisonerImage(offenderNo, fullSizeImage)
}
