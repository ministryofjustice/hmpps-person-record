package uk.gov.justice.digital.hmpps.personrecord.controller

class PersonRecordNotFoundException(personId: String): Exception(personId)
