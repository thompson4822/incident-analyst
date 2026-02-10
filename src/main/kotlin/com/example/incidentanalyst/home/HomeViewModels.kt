package com.example.incidentanalyst.home

data class DashboardStatsViewModel(
    val systemHealth: Double,
    val uptime: Double,
    val totalIncidents: Int,
    val activeIncidents: Int,
    val recentDiagnoses: Int,
    val meanResolutionTime: String?
)

data class HomeIncidentItemViewModel(
    val id: Long,
    val source: String,
    val severity: String,
    val severityColor: String,
    val title: String,
    val shortDescription: String,
    val status: String,
    val statusColor: String,
    val updatedAt: String,
    val tags: List<String>
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

data class ResponseStepViewModel(
    val step: Int,
    val title: String,
    val description: String,
    val status: String,
    val statusColor: String,
    val completed: Boolean
)

data class ResponsePlanViewModel(
    val name: String,
    val steps: List<ResponseStepViewModel>
)
