package uk.gov.justice.digital.hmpps.personrecord.api.controller.admin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.personrecord.config.WebTestBase
import uk.gov.justice.digital.hmpps.personrecord.model.types.ReligionCode

class ReligionCodeMappingControllerIntTest : WebTestBase() {

  @Test
  fun `maps all old codes to new codes`() {
    val cluster = createPersonKey()
    ReligionCode.entries.forEach {
      val person = createRandomProbationPersonDetails().copy(religion = it.description)
      val personEntity = createPerson(person)
      personEntity.personKey = cluster
      cluster.personEntities.add(personEntity)
      personRepository.save(personEntity)
    }

    sendPutRequestAsserted<Unit>(
      url = "/admin/religion-code-mappings",
      body = "",
      expectedStatus = HttpStatus.OK,
      roles = emptyList(),
    )

    awaitAssert {
      val cluster = personKeyRepository.findByPersonUUID(cluster.personUUID)!!
      cluster.personEntities.forEach { person ->
        assertDoesNotThrow { ReligionCode.valueOf(person.religion!!) }
      }
    }
  }

  @Test
  fun `maps all codes that are not mappable to new codes`() {
    val cluster = createPersonKey()
    val otherCodes = listOf("Christian", "Church of England (Anglican)", "Roman Catholic", "No Religion")
    otherCodes.forEach {
      val person = createRandomProbationPersonDetails().copy(religion = it)
      val personEntity = createPerson(person)
      personEntity.personKey = cluster
      cluster.personEntities.add(personEntity)
      personRepository.save(personEntity)
    }

    sendPutRequestAsserted<Unit>(
      url = "/admin/religion-code-mappings",
      body = "",
      expectedStatus = HttpStatus.OK,
      roles = emptyList(),
    )

    awaitAssert {
      val cluster = personKeyRepository.findByPersonUUID(cluster.personUUID)!!
      cluster.personEntities.forEach { person ->
        assertDoesNotThrow { ReligionCode.valueOf(person.religion!!) }
      }
    }
  }
}
