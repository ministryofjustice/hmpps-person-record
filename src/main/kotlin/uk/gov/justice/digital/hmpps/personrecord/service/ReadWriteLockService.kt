package uk.gov.justice.digital.hmpps.personrecord.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.locks.ReentrantReadWriteLock

@Service
class ReadWriteLockService {

  private val lock = ReentrantReadWriteLock()

  @Transactional(readOnly = true)
  fun <R> withReadLock(block: () -> R): R {
    lock.readLock().lock()
    try {
      return block()
    } finally {
      lock.readLock().unlock()
    }
  }

  @Transactional
  fun <R> withWriteLock(block: () -> R): R {
    lock.writeLock().lock()
    try {
      return block()
    } finally {
      lock.writeLock().unlock()
    }
  }
}