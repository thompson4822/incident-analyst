package com.example.incidentanalyst.runbook

data class RunbookFragmentResponseDto(
    val id: Long,
    val title: String,
    val content: String,
    val tags: String?
)
