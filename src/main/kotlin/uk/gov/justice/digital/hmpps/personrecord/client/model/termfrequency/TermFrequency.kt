package uk.gov.justice.digital.hmpps.personrecord.client.model.termfrequency

interface TermFrequency {
  fun getTerm(): String
  fun getFrequency(): Double
}
