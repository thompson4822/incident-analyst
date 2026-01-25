package com.example.incidentanalyst.diagnosis

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class DiagnosisService(
    private val diagnosisRepository: DiagnosisRepository
) {

    fun listRecent(limit: Int = 50): List<Diagnosis> =
        diagnosisRepository.findRecent(limit).map { it.toDomain() }

    fun getById(id: DiagnosisId): DiagnosisResult =
        diagnosisRepository.findById(id.value)?.toDomain()?.let { diagnosis ->
            DiagnosisResult.Success(diagnosis)
        } ?: DiagnosisResult.Failure(DiagnosisError.NotFound)

    @Transactional
    fun updateVerification(id: DiagnosisId, verification: DiagnosisVerification): DiagnosisResult {
        val entity = diagnosisRepository.findById(id.value) 
            ?: return DiagnosisResult.Failure(DiagnosisError.NotFound)
        
        entity.verification = when (verification) {
            DiagnosisVerification.VerifiedByHuman -> "VERIFIED"
            DiagnosisVerification.Unverified -> "UNVERIFIED"
        }
        
        // Transaction will auto-commit, entity is managed
        return DiagnosisResult.Success(entity.toDomain())
    }
}
