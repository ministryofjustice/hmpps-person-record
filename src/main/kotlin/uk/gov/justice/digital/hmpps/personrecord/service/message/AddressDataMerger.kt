package uk.gov.justice.digital.hmpps.personrecord.service.message

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person

@Component
class AddressDataMerger : PersonGranularDataMerger {
  override fun handleMerge(fromPerson: PersonEntity, toPerson: PersonEntity, person: Person) {
    toPerson.addresses.clear()
    val addresses = person.addresses.map {
      val addressEntity = AddressEntity.from(it)
      addressEntity.person = toPerson
      addressEntity
    }
    toPerson.addresses.addAll(addresses)
  }
}
