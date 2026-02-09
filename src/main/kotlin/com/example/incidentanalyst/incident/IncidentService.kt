package com.example.incidentanalyst.incident

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant

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

    @Transactional
    fun create(incident: Incident): Incident {
        val entity = incident.toEntity()
        incidentRepository.persistAndFlush(entity)
        return entity.toDomain()
    }

    @Transactional
    fun updateStatus(id: IncidentId, status: IncidentStatus): IncidentResult {
        val entity = incidentRepository.findById(id.value) ?: return IncidentResult.Failure(IncidentError.NotFound)
        entity.status = when (status) {
            IncidentStatus.Open -> "OPEN"
            IncidentStatus.Acknowledged -> "ACK"
            IncidentStatus.Resolved -> "RESOLVED"
            is IncidentStatus.Diagnosed -> "DIAGNOSED:${status.diagnosisId}"
        }
        entity.updatedAt = Instant.now()
        return IncidentResult.Success(entity.toDomain())
    }
}
