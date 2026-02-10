package com.example.incidentanalyst.home

data class StatsViewModel(
    val totalIncidents: Int,
    val activeIncidents: Int,
    val recentDiagnoses: Int,
    val meanResolutionTime: String?,
    val systemHealth: Double,
    val uptime: String
)

data class IncidentCardViewModel(
    val id: Long,
    val title: String,
    val source: String,
    val severity: String,
    val severityColor: String,
    val status: String,
    val statusColor: String,
    val createdAt: String,
    val shortDescription: String,
    val tags: List<String>,
    val updatedAt: String
)

data class ActiveIncidentViewModel(
    val id: Long,
    val title: String,
    val description: String,
    val severity: String,
    val severityColor: String,
    val status: String,
    val statusColor: String,
    val createdAt: String,
    val updatedAt: String,
    val source: String,
    val hasDiagnosis: Boolean,
    val diagnosisProgress: List<DiagnosisStepViewModel>,
    val verified: Boolean = false
)

data class DiagnosisStepViewModel(
    val step: Int,
    val title: String,
    val status: String,
    val statusColor: String
)

data class RunbookStepViewModel(
    val step: Int,
    val title: String,
    val description: String,
    val completed: Boolean
)

data class ActionCardViewModel(
    val label: String,
    val href: String,
    val description: String,
    val icon: String
)
