package com.example.missingpeople.repositor

import java.util.Date
import java.util.UUID

abstract class BaseEntity(
    open val id: UUID = UUID.randomUUID(),
    open val name: String,
    open val description: String,
    open val creationDate: Date = Date(),
    open val lastModified: Date = Date()
) {
    abstract fun getEntityType(): String
}
