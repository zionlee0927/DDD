package com.partnercommission.shared.domain

abstract class Entity<ID>(val id: ID) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as Entity<*>
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

abstract class AggregateRoot<ID>(id: ID) : Entity<ID>(id) {

    @Transient
    private val domainEvents: MutableList<Any> = mutableListOf()

    protected fun registerEvent(event: Any) {
        domainEvents.add(event)
    }

    fun getAndClearDomainEvents(): List<Any> {
        val events = domainEvents.toList()
        domainEvents.clear()
        return events
    }
}
