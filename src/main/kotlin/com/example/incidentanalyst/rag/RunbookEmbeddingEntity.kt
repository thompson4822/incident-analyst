package com.example.incidentanalyst.rag

import com.example.incidentanalyst.runbook.RunbookFragmentEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.GenerationType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "runbook_embeddings")
open class RunbookEmbeddingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fragment_id")
    open var fragment: RunbookFragmentEntity? = null,

    @Column(columnDefinition = "text")
    open var text: String = "",

    @Column(columnDefinition = "vector")
    open var embedding: ByteArray = ByteArray(0),

    @Column(name = "created_at")
    open var createdAt: Instant = Instant.now(),

    @Column(name = "source_type")
    open var sourceType: String = "OFFICIAL_RUNBOOK"
) : PanacheEntityBase
