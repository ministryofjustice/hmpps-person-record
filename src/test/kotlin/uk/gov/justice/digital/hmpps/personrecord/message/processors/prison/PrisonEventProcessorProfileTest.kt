package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.AddressEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.ContactEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.NOMIS
import uk.gov.justice.digital.hmpps.personrecord.service.person.PersonService
import uk.gov.justice.digital.hmpps.personrecord.service.person.PrisonPersonServiceProxy
import uk.gov.justice.digital.hmpps.personrecord.service.person.PrisonPersonServiceProxyDev
import uk.gov.justice.digital.hmpps.personrecord.service.person.PrisonPersonServiceProxyProd

@SpringJUnitConfig(PrisonPersonServiceProxyProd::class, PrisonPersonServiceProxyDev::class)
class PrisonEventProcessorProfileTest {
  private val person = Person(sourceSystem = NOMIS)
  private val personEntity = PersonEntity.new(NOMIS)

  @MockitoBean
  private lateinit var personService: PersonService

  @Nested
  @ActiveProfiles("prod")
  inner class ProdProfileTest(@Autowired private val prisonPersonServiceProxy: PrisonPersonServiceProxy) {

    @Test
    fun `uses the correct prison person service proxy for the active profile`() {
      assertThat(prisonPersonServiceProxy).isInstanceOf(PrisonPersonServiceProxyProd::class.java)
    }

    @Test
    fun `passes emptySet for the childrenToIgnore`() {
      prisonPersonServiceProxy.processPerson(person, { personEntity })

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(emptySet()),
        findPerson = any(),
      )
    }
  }

  @Nested
  @ActiveProfiles("preprod")
  inner class PreProdProfileTest(@Autowired private val prisonPersonServiceProxy: PrisonPersonServiceProxy) {

    @Test
    fun `uses the correct prison person service proxy for the active profile`() {
      assertThat(prisonPersonServiceProxy).isInstanceOf(PrisonPersonServiceProxyDev::class.java)
    }

    @Test
    fun `passes set of addresses and contacts for the childrenToIgnore`() {
      prisonPersonServiceProxy.processPerson(person, { personEntity })

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
        findPerson = any(),
      )
    }
  }

  @Nested
  @ActiveProfiles("dev")
  inner class DevProfileTest(@Autowired private val prisonPersonServiceProxy: PrisonPersonServiceProxy) {

    @Test
    fun `uses the correct prison person service proxy for the active profile`() {
      assertThat(prisonPersonServiceProxy).isInstanceOf(PrisonPersonServiceProxyDev::class.java)
    }

    @Test
    fun `passes set of addresses and contacts for the childrenToIgnore`() {
      prisonPersonServiceProxy.processPerson(person, { personEntity })

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
        findPerson = any(),
      )
    }
  }
}
