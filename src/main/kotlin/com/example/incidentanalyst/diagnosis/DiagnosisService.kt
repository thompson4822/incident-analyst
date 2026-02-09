package com.example.incidentanalyst.diagnosis

import com.example.incidentanalyst.common.Either
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class DiagnosisService(
    private val diagnosisRepository: DiagnosisRepository
) {

    fun listRecent(limit: Int = 50): List<Diagnosis> =
        diagnosisRepository.findRecent(limit).map { it.toDomain() }

    fun getByIncidentId(incidentId: Long): Diagnosis? =
        diagnosisRepository.findByIncidentId(incidentId)?.toDomain()

    fun getById(id: DiagnosisId): Either<DiagnosisError, Diagnosis> =
        diagnosisRepository.findById(id.value)?.toDomain()?.let { diagnosis ->
            Either.Right(diagnosis)
        } ?: Either.Left(DiagnosisError.NotFound)

    @Transactional
    fun updateVerification(id: DiagnosisId, verification: DiagnosisVerification): Either<DiagnosisError, Diagnosis> {
        val entity = diagnosisRepository.findById(id.value) 
            ?: return Either.Left(DiagnosisError.NotFound)
        
        entity.verification = when (verification) {
            DiagnosisVerification.VerifiedByHuman -> "VERIFIED"
            DiagnosisVerification.Unverified -> "UNVERIFIED"
        }
        
        // Transaction will auto-commit, entity is managed
        return Either.Right(entity.toDomain())
    }
}
