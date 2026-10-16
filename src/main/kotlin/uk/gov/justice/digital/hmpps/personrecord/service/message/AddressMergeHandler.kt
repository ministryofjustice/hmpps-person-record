package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.extensions.toUkZonedDateTime
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressStatusCode
import java.time.LocalDate

@Component
class AddressMergeHandler : MergeHandler {
  override fun handleMerge(fromPerson: PersonEntity, toPerson: PersonEntity) {
    val fromPersonAddresses = fromPerson.addresses.onEach {
      it.person = toPerson
      if (it.statusCode == AddressStatusCode.M) {
        it.statusCode = AddressStatusCode.P
        it.endDate = LocalDate.now().toUkZonedDateTime()
      }
    }
    toPerson.addresses.addAll(fromPersonAddresses)
  }
}
