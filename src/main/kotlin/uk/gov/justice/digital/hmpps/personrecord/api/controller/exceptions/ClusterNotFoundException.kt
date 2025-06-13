package uk.gov.justice.digital.hmpps.personrecord.api.controller.exceptions

import java.util.UUID

class ClusterNotFoundException(uuid: UUID) : Exception(uuid.toString())
