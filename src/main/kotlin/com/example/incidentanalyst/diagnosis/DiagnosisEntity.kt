package com.example.incidentanalyst.diagnosis

import com.example.incidentanalyst.incident.IncidentEntity
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
@Table(name = "diagnoses")
open class DiagnosisEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    open var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    open var incident: IncidentEntity? = null,

    @Column(name = "suggested_root_cause", columnDefinition = "text")
    open var suggestedRootCause: String = "",

    @Column(name = "remediation_steps", columnDefinition = "text")
    open var remediationSteps: String = "",

    @Column(name = "structured_steps", columnDefinition = "text")
    open var structuredSteps: String? = null,

    open var confidence: String = "",
    open var verification: String = "UNVERIFIED",
    @Column(name = "created_at")
    open var createdAt: Instant = Instant.now(),
    @Column(name = "verified_at")
    open var verifiedAt: Instant? = null,
    @Column(name = "verified_by")
    open var verifiedBy: String? = null
) : PanacheEntityBase
