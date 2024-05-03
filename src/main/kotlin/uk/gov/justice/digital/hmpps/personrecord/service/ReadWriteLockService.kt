package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.locks.ReentrantReadWriteLock

@Service
class ReadWriteLockService {

  private val lock = ReentrantReadWriteLock()

  @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
  fun <T> withReadLock(block: () -> T?): T? {
    lock.readLock().lock()
    try {
      return block()
    } finally {
      lock.readLock().unlock()
    }
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  fun <T> withWriteLock(block: () -> T?): T? {
    lock.writeLock().lock()
    try {
      return block()
    } finally {
      lock.writeLock().unlock()
    }
  }
}
