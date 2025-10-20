package uk.gov.justice.digital.hmpps.personrecord.service.person

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity
import uk.gov.justice.digital.hmpps.personrecord.jpa.entity.PersonEntity.Companion.exists
import uk.gov.justice.digital.hmpps.personrecord.model.person.Person
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType.DELIUS
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonCreated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.person.PersonUpdated
import uk.gov.justice.digital.hmpps.personrecord.service.cprdomainevents.events.personkey.DeliusMergeRequest
import uk.gov.justice.digital.hmpps.personrecord.service.message.recluster.ReclusterService
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonChainable
import uk.gov.justice.digital.hmpps.personrecord.service.person.factories.PersonFactory
import uk.gov.justice.digital.hmpps.personrecord.service.search.PersonMatchService

@Component
class PersonService(
  private val personFactory: PersonFactory,
  private val personKeyService: PersonKeyService,
  private val personMatchService: PersonMatchService,
  private val reclusterService: ReclusterService,
  private val publisher: ApplicationEventPublisher,
) {

  fun processPerson(
    person: Person,
    findPerson: () -> PersonEntity?,
  ): PersonEntity = findPerson().exists(
    no = {
      create(person)
    },
    yes = {
      update(person, it)
    },
  ).also {
    clusterHasMoreThanOneDeliusRecord(it)
  }

  private fun clusterHasMoreThanOneDeliusRecord(entity: PersonEntity) {
    entity.personKey?.let { personKey ->
      if (personKey.personEntities.filter { person -> person.sourceSystem == DELIUS }.size > 1) {
        publisher.publishEvent(DeliusMergeRequest(entity, personKey))
      }
    }
  }

  private fun create(person: Person): PersonEntity {
    val ctx = personFactory.create(person)
      .saveToPersonMatch()
      .linkToPersonKey()
    publisher.publishEvent(PersonCreated(ctx.personEntity))
    return ctx.personEntity
  }

  private fun update(person: Person, personEntity: PersonEntity): PersonEntity {
    val ctx = personFactory.update(person, personEntity)
      .saveToPersonMatch()
      .reclusterIf { ctx -> person.behaviour.reclusterOnUpdate && ctx.matchingFieldsChanged }
    publisher.publishEvent(PersonUpdated(ctx.personEntity, ctx.matchingFieldsChanged))
    return ctx.personEntity
  }

  private fun PersonChainable.saveToPersonMatch(): PersonChainable {
    when {
      this.matchingFieldsChanged -> personMatchService.saveToPersonMatch(this.personEntity)
    }
    return this
  }

  private fun PersonChainable.linkToPersonKey(): PersonChainable {
    when {
      this.linkOnCreate -> personKeyService.linkRecordToPersonKey(this.personEntity)
    }
    return this
  }

  private fun PersonChainable.reclusterIf(condition: (ctx: PersonChainable) -> Boolean): PersonChainable {
    when {
      condition(this) -> this.personEntity.personKey?.let { reclusterService.recluster(this.personEntity) }
    }
    return this
  }
}
