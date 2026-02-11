package uk.gov.justice.digital.hmpps.personrecord.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import uk.gov.justice.digital.hmpps.personrecord.model.person.AddressUsage
import uk.gov.justice.digital.hmpps.personrecord.model.types.AddressUsageCode

@Entity
@Table(name = "address_usage")
class AddressUsageEntity(
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Long? = null,

  @ManyToOne(optional = false)
  @JoinColumn(
    name = "fk_address_id",
    referencedColumnName = "id",
    nullable = false,
  )
  var address: AddressEntity? = null,

  @Column(name = "usage_code")
  @Enumerated(EnumType.STRING)
  val usageCode: AddressUsageCode,

  @Column(name = "active")
  val active: Boolean,

  @Version
  var version: Int = 0,
) {
  companion object {
    fun from(addressUsage: AddressUsage) = AddressUsageEntity(
      usageCode = addressUsage.addressUsageCode,
      active = addressUsage.isActive,
    )
  }
}
