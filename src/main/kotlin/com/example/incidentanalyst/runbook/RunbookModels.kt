package com.example.incidentanalyst.runbook

import java.time.Instant

@JvmInline
value class RunbookFragmentId(val value: Long)

data class RunbookFragment(
    val id: RunbookFragmentId,
    val title: String,
    val content: String,
    val tags: String?,
    val createdAt: Instant
)

sealed interface RunbookFragmentError {
    data object NotFound : RunbookFragmentError
    data object ValidationFailed : RunbookFragmentError
}

sealed interface RunbookFragmentResult {
    data class Success(val fragment: RunbookFragment) : RunbookFragmentResult
    data class Failure(val error: RunbookFragmentError) : RunbookFragmentResult
}

fun RunbookFragmentEntity.toDomain(): RunbookFragment =
    RunbookFragment(
        id = RunbookFragmentId(requireNotNull(id)),
        title = title,
        content = content,
        tags = tags,
        createdAt = createdAt
    )
