package com.example.incidentanalyst.runbook

import java.time.Instant

data class RunbookListViewModel(
    val runbooks: List<RunbookListItemViewModel>,
    val totalCount: Int,
    val categories: List<String>,
    val filters: RunbookFiltersViewModel
)

data class RunbookFiltersViewModel(
    val query: String? = null,
    val category: String? = null
)

data class RunbookListItemViewModel(
    val id: Long,
    val title: String,
    val shortContent: String,
    val tags: List<String>,
    val createdAt: String,
    val stepCount: Int,
    val version: String,
    val severity: String,
    val severityColor: String
)

data class RunbookDetailViewModel(
    val id: Long,
    val title: String,
    val content: String,
    val tags: List<String>,
    val createdAt: String,
    val version: String,
    val severity: String,
    val severityColor: String,
    val category: String,
    val steps: List<RunbookStepViewModel>
)

data class RunbookStepViewModel(
    val order: Int,
    val title: String,
    val description: String,
    val completed: Boolean
)
