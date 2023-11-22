package uk.gov.justice.digital.hmpps.personrecord.model.libra

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.OptBoolean
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
data class LibraHearing(
  val urn: String? = null,
  val name: LibraName? = null,
  val defendantName: String? = null,
  val defendantSex: String? = null,
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy", lenient = OptBoolean.TRUE)
  @JsonDeserialize(using = LocalDateDeserializer::class)
  val defendantDob: LocalDate? = null,
  val defendantAddress: LibraAddress? = null,
  val cro: String? = null,
  val pnc: String? = null,
  val nationality1: String? = null,
  val nationality2: String? = null,
)
