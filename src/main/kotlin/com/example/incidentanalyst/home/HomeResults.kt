package com.example.incidentanalyst.home

sealed interface HomeResult {
    data class Success(
        val stats: StatsViewModel,
        val incidents: List<IncidentCardViewModel>,
        val activeIncident: ActiveIncidentViewModel?,
        val runbookSteps: List<RunbookStepViewModel>
    ) : HomeResult
    data class Failure(val error: HomeError) : HomeResult
}

sealed interface HomeError {
    data object DataUnavailable : HomeError
}
