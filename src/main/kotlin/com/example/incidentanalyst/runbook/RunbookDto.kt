package com.example.incidentanalyst.runbook

data class RunbookFragmentResponseDto(
    val id: Long,
    val title: String,
    val content: String,
    val tags: String?
)

data class RunbookFragmentUpdateRequestDto(
    val title: String,
    val content: String,
    val tags: String?
)

data class RunbookFragmentCreateRequestDto(
    val title: String,
    val content: String,
    val tags: String?
)

fun RunbookFragment.toResponseDto(): RunbookFragmentResponseDto =
    RunbookFragmentResponseDto(
        id = id.value,
        title = title,
        content = content,
        tags = tags
    )
