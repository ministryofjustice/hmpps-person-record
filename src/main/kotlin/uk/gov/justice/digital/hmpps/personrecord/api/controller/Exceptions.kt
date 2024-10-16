package uk.gov.justice.digital.hmpps.personrecord.api.controller

class PersonRecordNotFoundException(personId: String): Exception(personId)
