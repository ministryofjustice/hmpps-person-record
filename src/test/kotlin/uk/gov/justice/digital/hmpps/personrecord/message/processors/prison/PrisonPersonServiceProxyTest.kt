package uk.gov.justice.digital.hmpps.personrecord.message.processors.prison

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.personrecord.service.person.PrisonPersonServiceProxyImpl
import uk.gov.justice.digital.hmpps.personrecord.service.person.PrisonPersonServiceProxyProd

@Suppress("SpringJavaInjectionPointsAutowiringInspection")
@SpringJUnitConfig(PrisonPersonServiceProxyImpl::class, PrisonPersonServiceProxyProd::class)
class PrisonPersonServiceProxyTest {
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
    fun `delegates update to the personService`() {
      prisonPersonServiceProxy.processPersonUpdate(person)

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
      )
    }

    @Test
    fun `delegates create to the personService`() {
      prisonPersonServiceProxy.processPersonCreate(person)

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
      )
    }
  }

  @Nested
  @ActiveProfiles("preprod")
  inner class PreProdProfileTest(@Autowired private val prisonPersonServiceProxy: PrisonPersonServiceProxy) {

    @Test
    fun `uses the correct prison person service proxy for the active profile`() {
      assertThat(prisonPersonServiceProxy).isInstanceOf(PrisonPersonServiceProxyProd::class.java)
    }

    @Test
    fun `delegates update to the personService`() {
      prisonPersonServiceProxy.processPersonUpdate(person)

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
      )
    }

    @Test
    fun `delegates create to the personService`() {
      prisonPersonServiceProxy.processPersonCreate(person)

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
      )
    }
  }

  @Nested
  @ActiveProfiles("dev")
  inner class DevProfileTest(@Autowired private val prisonPersonServiceProxy: PrisonPersonServiceProxy) {

    @Test
    fun `uses the correct prison person service proxy for the active profile`() {
      assertThat(prisonPersonServiceProxy).isInstanceOf(PrisonPersonServiceProxyProd::class.java)
    }

    @Test
    fun `delegates update to the personService`() {
      prisonPersonServiceProxy.processPersonUpdate(person)

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
      )
    }

    // TODO remove when we turn off create via search event
    @Test
    fun `delegates create to the personService`() {
      prisonPersonServiceProxy.processPersonCreate(person)

      verify(personService).processPerson(
        person = eq(person),
        childrenToIgnore = eq(setOf(AddressEntity::class, ContactEntity::class)),
      )
    }

    // TODO uncomment out when we turn off create via search event
//    @Test
//    fun `create is a no-op`() {
//      prisonPersonServiceProxy.processPersonCreate(person) { personEntity }
//
//      verify(personService, never()).processPerson(
//        person = any(),
//        childrenToIgnore = any(),
//        findPerson = any(),
//      )
//    }
  }
}
