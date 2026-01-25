package com.example.incidentanalyst.runbook

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.GenerationType
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "runbook_fragments")
open class RunbookFragmentEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    open var title: String = "",

    @Column(columnDefinition = "text")
    open var content: String = "",

    @Column(columnDefinition = "jsonb")
    open var tags: String? = null,

    @Column(name = "created_at")
    open var createdAt: Instant = Instant.now()
) : PanacheEntityBase
