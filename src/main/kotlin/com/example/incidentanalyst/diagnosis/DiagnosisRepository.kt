package com.example.incidentanalyst.diagnosis

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class DiagnosisRepository : PanacheRepository<DiagnosisEntity> {

    fun findRecent(limit: Int = 50): List<DiagnosisEntity> =
        find("order by createdAt desc").page(0, limit).list()

    fun findByIncidentId(incidentId: Long): DiagnosisEntity? =
        find("incident.id", incidentId).firstResult()
}
