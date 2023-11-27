package uk.gov.justice.digital.hmpps.personrecord.model.hmcts.commonplatform

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.personrecord.model.hmcts.event.CommonPlatformHearingEvent
import uk.gov.justice.digital.hmpps.personrecord.service.helper.commonPlatformHearing

class CommonPlatformHearingEventTest {

  private lateinit var objectMapper: ObjectMapper

  @BeforeEach
  fun setUp() {
    objectMapper = jacksonObjectMapper().findAndRegisterModules()
  }

  @Test
  fun `should convert to common platform event from the message`() {
    // given
    val cpHearingMessage = commonPlatformHearing()

    // when
    val commonPlatformHearingEvent =
      objectMapper.readValue(cpHearingMessage, CommonPlatformHearingEvent::class.java)

    // then
    assertThat(commonPlatformHearingEvent).isNotNull()
    assertThat(commonPlatformHearingEvent.hearing.prosecutionCases).isNotNull()
    assertThat(commonPlatformHearingEvent.hearing.prosecutionCases[0].defendants.size).isEqualTo(2)
  }
}
