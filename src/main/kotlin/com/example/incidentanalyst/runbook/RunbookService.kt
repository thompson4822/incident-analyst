package com.example.incidentanalyst.runbook

import jakarta.enterprise.context.ApplicationScoped
import jakarta.transaction.Transactional

@ApplicationScoped
class RunbookService(
    private val runbookFragmentRepository: RunbookFragmentRepository
) {

    fun listRecent(limit: Int = 50): List<RunbookFragment> =
        runbookFragmentRepository.findRecent(limit).map { it.toDomain() }

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

    fun getById(id: RunbookFragmentId): RunbookFragmentResult =
        runbookFragmentRepository.findById(id.value)?.toDomain()?.let { fragment ->
            RunbookFragmentResult.Success(fragment)
        } ?: RunbookFragmentResult.Failure(RunbookFragmentError.NotFound)

    @Transactional
    fun updateFragment(
        id: RunbookFragmentId,
        title: String,
        content: String,
        tags: String?
    ): RunbookFragmentResult {
        // Validate input
        if (title.isBlank() || content.isBlank()) {
            return RunbookFragmentResult.Failure(RunbookFragmentError.ValidationFailed)
        }
        
        val entity = runbookFragmentRepository.findById(id.value)
            ?: return RunbookFragmentResult.Failure(RunbookFragmentError.NotFound)
        
        entity.title = title
        entity.content = content
        entity.tags = tags
        
        // Transaction will auto-commit, entity is managed
        return RunbookFragmentResult.Success(entity.toDomain())
    }
}
