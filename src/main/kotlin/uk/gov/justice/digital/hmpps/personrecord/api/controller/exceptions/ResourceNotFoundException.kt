package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

import java.util.UUID

class ResourceNotFoundException(uuid: UUID) : Exception(uuid.toString())
