package com.example.incidentanalyst.runbook

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RunbookFragmentResponseDto(
    val id: Long,
    val title: String,
    val content: String,
    val tags: String?
)

data class RunbookFragmentUpdateRequestDto(
    @field:NotBlank(message = "title is required and must not be blank")
    @field:Size(max = 255, message = "title must not exceed 255 characters")
    val title: String,

    @field:NotBlank(message = "content is required and must not be blank")
    val content: String,

    val tags: String?
)

data class RunbookFragmentCreateRequestDto(
    @field:NotBlank(message = "title is required and must not be blank")
    @field:Size(max = 255, message = "title must not exceed 255 characters")
    val title: String,

    @field:NotBlank(message = "content is required and must not be blank")
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
