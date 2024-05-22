package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.libra

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.LibraHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.test.messages.libraHearing

class LibraHearingEventTest {

  private lateinit var objectMapper: ObjectMapper

  @BeforeEach
  fun setUp() {
    objectMapper = jacksonObjectMapper().findAndRegisterModules()
  }

  @Test
  fun `should convert to libra hearing event from the message`() {
    // given
    val libraHearingMessage = libraHearing()

    val libraHearingEvent = objectMapper.readValue<LibraHearingEvent>(libraHearingMessage)

    // then
    assertThat(libraHearingEvent).isNotNull()
  }
}
