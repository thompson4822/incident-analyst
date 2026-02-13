package com.example.incidentanalyst.incident

import com.example.incidentanalyst.common.Either
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant

@ApplicationScoped
class IncidentService(
    private val incidentRepository: IncidentRepository
) {

    fun listRecent(limit: Int = 50): List<Incident> =
        incidentRepository.findRecent(limit).map { it.toDomain() }

    fun search(
        query: String?,
        status: String?,
        severity: String?,
        source: IncidentSource?,
        limit: Int = 50
    ): List<Incident> =
        incidentRepository.findByFilters(query, status, severity, source?.toPersistenceString(), limit)
            .map { it.toDomain() }

    fun getById(id: IncidentId): Either<IncidentError, Incident> =
        incidentRepository.findById(id.value)?.toDomain()?.let { incident ->
            Either.Right(incident)
        } ?: Either.Left(IncidentError.NotFound)

    @Transactional
    fun create(incident: Incident): Incident {
        val entity = incident.toEntity()
        incidentRepository.persistAndFlush(entity)
        return entity.toDomain()
    }

    @Transactional
    fun updateStatus(id: IncidentId, status: IncidentStatus): Either<IncidentError, Incident> {
        val entity = incidentRepository.findById(id.value) ?: return Either.Left(IncidentError.NotFound)
        entity.status = when (status) {
            IncidentStatus.Open -> "OPEN"
            IncidentStatus.Acknowledged -> "ACK"
            IncidentStatus.Resolved -> "RESOLVED"
            is IncidentStatus.Diagnosed -> "DIAGNOSED:${status.diagnosisId}"
        }
        entity.updatedAt = Instant.now()
        return Either.Right(entity.toDomain())
    }

    @Transactional
    fun resolve(id: IncidentId, resolutionText: String): Either<IncidentError, Incident> {
        val entity = incidentRepository.findById(id.value) ?: return Either.Left(IncidentError.NotFound)
        entity.status = "RESOLVED"
        entity.resolutionText = resolutionText
        entity.updatedAt = Instant.now()
        return Either.Right(entity.toDomain())
    }
}
