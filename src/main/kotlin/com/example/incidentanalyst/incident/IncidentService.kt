package com.example.incidentanalyst.incident

import jakarta.enterprise.context.ApplicationScoped

@ApplicationScoped
class IncidentService(
    private val incidentRepository: IncidentRepository
) {

    fun listRecent(limit: Int = 50): List<Incident> =
        incidentRepository.findRecent(limit).map { it.toDomain() }

    fun getById(id: IncidentId): IncidentResult =
        incidentRepository.findById(id.value)?.toDomain()?.let { incident ->
            IncidentResult.Success(incident)
        } ?: IncidentResult.Failure(IncidentError.NotFound)
}
