package com.example.incidentanalyst.diagnosis

import com.example.incidentanalyst.common.Either
import com.example.incidentanalyst.rag.EmbeddingError
import com.example.incidentanalyst.rag.EmbeddingService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional
import java.time.Instant

@ApplicationScoped
class DiagnosisService(
    private val diagnosisRepository: DiagnosisRepository,
    private val embeddingService: EmbeddingService
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

    @Transactional
    fun verify(id: DiagnosisId, user: String): Either<DiagnosisError, Diagnosis> {
        val entity = diagnosisRepository.findById(id.value)
            ?: return Either.Left(DiagnosisError.NotFound)

        // Update verification status
        entity.verification = "VERIFIED"
        entity.verifiedAt = Instant.now()
        entity.verifiedBy = user

        // Persist the verified diagnosis embedding
        val embeddingResult = embeddingService.embedVerifiedDiagnosis(id)
        if (embeddingResult.isLeft()) {
            // Embedding failure should not prevent the diagnosis from being verified
            // Log the error but continue
        }

        // Transaction will auto-commit, entity is managed
        return Either.Right(entity.toDomain())
    }
}
