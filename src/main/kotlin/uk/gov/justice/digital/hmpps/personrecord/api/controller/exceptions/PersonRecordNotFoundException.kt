package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

class PersonRecordNotFoundException(personId: String) : Exception(personId)
