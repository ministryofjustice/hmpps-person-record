package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.personrecord.model.types.SourceSystemType
import java.util.concurrent.locks.ReentrantReadWriteLock

@Component
class ReadWriteLockService {

  private val lockMap = mutableMapOf<SourceSystemType, ReentrantReadWriteLock>()

  private fun getLock(sourceSystem: SourceSystemType): ReentrantReadWriteLock {
    return synchronized(lockMap) {
      lockMap.getOrPut(sourceSystem) { ReentrantReadWriteLock() }
    }
  }

  fun <T> withWriteLock(sourceSystem: SourceSystemType, block: () -> T): T {
    val lock = getLock(sourceSystem)
    lock.writeLock().lock()
    return try {
      block()
    } finally {
      lock.writeLock().unlock()
    }
  }
}
