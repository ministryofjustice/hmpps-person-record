package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import java.util.concurrent.locks.ReentrantReadWriteLock

@Service
class ReadWriteLockService {

  private val lock = ReentrantReadWriteLock()

  fun <T> withReadLock(block: () -> T): T {
    lock.readLock().lock()
    try {
      return block()
    } finally {
      lock.readLock().unlock()
    }
  }

  fun <T> withWriteLock(block: () -> T?): T? {
    lock.writeLock().lock()
    return try {
      block()
    } finally {
      lock.writeLock().unlock()
    }
  }
}
