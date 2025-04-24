package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

class CircularMergeException : Exception("Circular merging exception: Target cluster cannot be merged into Source cluster")
