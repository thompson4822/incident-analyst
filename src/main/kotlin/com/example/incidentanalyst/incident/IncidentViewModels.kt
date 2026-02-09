package com.example.incidentanalyst.incident

import com.example.incidentanalyst.home.DiagnosisStepViewModel
import com.example.incidentanalyst.home.RunbookStepViewModel

data class IncidentDetailViewModel(
    val id: Long,
    val title: String,
    val description: String,
    val source: String,
    val severity: String,
    val severityColor: String,
    val status: String,
    val statusColor: String,
    val createdAt: String,
    val updatedAt: String,
    val duration: String,
    val tags: List<String>,
    val assignee: String?,
    val diagnosis: DiagnosisViewModel?,
    val timeline: List<TimelineEventViewModel>
)

data class IncidentListViewModel(
    val incidents: List<IncidentListItemViewModel>,
    val totalCount: Int,
    val activeCount: Int,
    val filters: IncidentFiltersViewModel
)

data class IncidentListItemViewModel(
    val id: Long,
    val title: String,
    val shortDescription: String,
    val source: String,
    val severity: String,
    val severityColor: String,
    val status: String,
    val statusColor: String,
    val createdAt: String,
    val updatedAt: String,
    val tags: List<String>
)

data class IncidentFiltersViewModel(
    val query: String? = null,
    val severity: String? = null,
    val status: String? = null,
    val source: String? = null
)

data class DiagnosisViewModel(
    val id: Long,
    val rootCause: String,
    val steps: List<String>,
    val confidence: String,
    val confidenceColor: String,
    val progress: List<DiagnosisStepViewModel>
)

data class TimelineEventViewModel(
    val timestamp: String,
    val action: String,
    val description: String,
    val color: String,
    val icon: String
)
