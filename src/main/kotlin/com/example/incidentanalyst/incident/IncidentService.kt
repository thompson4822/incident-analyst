package com.example.incidentanalyst.incident

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class IncidentService(
    private val incidentRepository: IncidentRepository
) {

    fun listRecent(limit: Int = 50): List<Incident> =
        incidentRepository.findRecent(limit).map { it.toDomain() }
}
