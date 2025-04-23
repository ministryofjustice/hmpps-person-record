package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

class CircularMergeException(exception: String) : Exception("Circular merging exception: $exception")
