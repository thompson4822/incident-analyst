package com.example.incidentanalyst.home

sealed interface HomeResult {
    data class Success(
        val stats: DashboardStatsViewModel,
        val incidents: List<HomeIncidentItemViewModel>,
        val activeIncident: ActiveIncidentViewModel?,
        val runbookSteps: List<ResponseStepViewModel>
    ) : HomeResult
    data class Failure(val error: HomeError) : HomeResult
}

sealed interface HomeError {
    data object DataUnavailable : HomeError
}
