package com.example.incidentanalyst.home

import com.example.incidentanalyst.diagnosis.DiagnosisService
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.web.toDaisyColor
import com.example.incidentanalyst.web.toDisplayString
import com.example.incidentanalyst.web.toRelativeTime
import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/")
class HomeResource {

    @Inject
    @Location("home.html")
    lateinit var homeTemplate: Template

    @Inject
    @Location("home/fragments/stats.html")
    lateinit var statsTemplate: Template

    @Inject
    @Location("home/fragments/recent-incidents.html")
    lateinit var recentIncidentsTemplate: Template

    @Inject
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var diagnosisService: DiagnosisService

    @GET
    @Produces(MediaType.TEXT_HTML)
    fun home(): TemplateInstance = homeTemplate.instance()

    @GET
    @Path("/home/stats")
    @Produces(MediaType.TEXT_HTML)
    fun stats(): TemplateInstance {
        val result = loadHomeData()
        val stats = when (result) {
            is HomeResult.Success -> result.stats
            is HomeResult.Failure -> createEmptyStats()
        }
        return statsTemplate.data("stats", stats)
    }

    @GET
    @Path("/home/recent-incidents")
    @Produces(MediaType.TEXT_HTML)
    fun recentIncidents(): TemplateInstance {
        val result = loadHomeData()
        val incidents = when (result) {
            is HomeResult.Success -> result.incidents
            is HomeResult.Failure -> emptyList()
        }
        return recentIncidentsTemplate.data("incidents", incidents)
    }

    private fun loadHomeData(): HomeResult {
        return try {
            val incidents = incidentService.listRecent(limit = 10)
            val diagnoses = diagnosisService.listRecent(limit = 50)

            val activeIncidents = incidents.count {
                it.status != com.example.incidentanalyst.incident.IncidentStatus.Resolved
            }

            val stats = StatsViewModel(
                totalIncidents = incidents.size,
                activeIncidents = activeIncidents,
                recentDiagnoses = diagnoses.size,
                meanResolutionTime = null
            )

            val incidentCards = incidents.map { incident ->
                IncidentCardViewModel(
                    id = incident.id.value,
                    title = incident.title,
                    source = incident.source,
                    severity = incident.severity.name,
                    severityColor = incident.severity.toDaisyColor(),
                    status = incident.status.toDisplayString(),
                    statusColor = incident.status.toDaisyColor(),
                    createdAt = incident.createdAt.toRelativeTime()
                )
            }

            HomeResult.Success(stats, incidentCards)
        } catch (e: Exception) {
            HomeResult.Failure(HomeError.DataUnavailable)
        }
    }

    private fun createEmptyStats(): StatsViewModel = StatsViewModel(
        totalIncidents = 0,
        activeIncidents = 0,
        recentDiagnoses = 0,
        meanResolutionTime = null
    )
}
