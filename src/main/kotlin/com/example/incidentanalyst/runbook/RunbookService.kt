package com.example.incidentanalyst.runbook

import com.example.incidentanalyst.common.Either
import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class RunbookService(
    private val runbookFragmentRepository: RunbookFragmentRepository
) {

    fun listRecent(limit: Int = 50): List<RunbookFragment> =
        runbookFragmentRepository.findRecent(limit).map { it.toDomain() }

    fun search(query: String?, tag: String?, limit: Int = 50): List<RunbookFragment> =
        runbookFragmentRepository.findByFilters(query, tag, limit).map { it.toDomain() }

    @Transactional
    fun createFragment(title: String, content: String, tags: String?): RunbookFragment {
        val entity = RunbookFragmentEntity(
            title = title,
            content = content,
            tags = tags
        )
        runbookFragmentRepository.persistAndFlush(entity)
        return entity.toDomain()
    }

    fun getById(id: RunbookFragmentId): Either<RunbookFragmentError, RunbookFragment> =
        runbookFragmentRepository.findById(id.value)?.toDomain()?.let { fragment ->
            Either.Right(fragment)
        } ?: Either.Left(RunbookFragmentError.NotFound)

    @Transactional
    fun updateFragment(
        id: RunbookFragmentId,
        title: String,
        content: String,
        tags: String?
    ): Either<RunbookFragmentError, RunbookFragment> {
        // Validate input
        if (title.isBlank() || content.isBlank()) {
            return Either.Left(RunbookFragmentError.ValidationFailed)
        }
        
        val entity = runbookFragmentRepository.findById(id.value)
            ?: return Either.Left(RunbookFragmentError.NotFound)
        
        entity.title = title
        entity.content = content
        entity.tags = tags
        
        // Transaction will auto-commit, entity is managed
        return Either.Right(entity.toDomain())
    }
}
