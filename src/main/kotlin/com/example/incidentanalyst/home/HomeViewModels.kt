package com.example.incidentanalyst.home

data class StatsViewModel(
    val totalIncidents: Int,
    val activeIncidents: Int,
    val recentDiagnoses: Int,
    val meanResolutionTime: String?
)

data class IncidentCardViewModel(
    val id: Long,
    val title: String,
    val source: String,
    val severity: String,
    val severityColor: String,
    val status: String,
    val statusColor: String,
    val createdAt: String
)

data class ActionCardViewModel(
    val label: String,
    val href: String,
    val description: String,
    val icon: String
)
