package com.example.incidentanalyst.incident

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.GenerationType
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "incidents")
open class IncidentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,
    open var source: String = "",
    open var title: String = "",
    @Column(columnDefinition = "text")
    open var description: String = "",
    open var severity: String = "",
    open var status: String = "OPEN",
    @Column(name = "created_at")
    open var createdAt: Instant = Instant.now(),
    @Column(name = "updated_at")
    open var updatedAt: Instant = Instant.now()
) : PanacheEntityBase
