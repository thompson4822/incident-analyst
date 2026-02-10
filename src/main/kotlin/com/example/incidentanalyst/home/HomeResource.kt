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
import java.time.Duration
import java.time.Instant

import com.example.incidentanalyst.agent.IncidentDiagnosisService
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentStatus
import jakarta.ws.rs.POST
import jakarta.ws.rs.PathParam

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
    @Location("home/fragments/incident-table.html")
    lateinit var incidentTableTemplate: Template

    @Inject
    @Location("home/fragments/active-incident.html")
    lateinit var activeIncidentTemplate: Template

    @Inject
    @Location("home/fragments/runbook-sidebar.html")
    lateinit var runbookSidebarTemplate: Template

    @Inject
    lateinit var incidentService: IncidentService

    @Inject
    lateinit var diagnosisService: DiagnosisService

    @Inject
    lateinit var incidentDiagnosisService: IncidentDiagnosisService

    @GET
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun home(): TemplateInstance = homeTemplate.instance()

    @POST
    @Path("/home/incidents/{id}/diagnose")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun diagnose(@PathParam("id") id: Long): TemplateInstance {
        val incidentId = IncidentId(id)
        incidentDiagnosisService.diagnose(incidentId)
        return activeIncident()
    }

    @POST
    @Path("/home/incidents/{id}/verify-diagnosis")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun verifyDiagnosis(@PathParam("id") id: Long): TemplateInstance {
        val incidentId = IncidentId(id)
        val diagnosis = diagnosisService.getByIncidentId(id)
        if (diagnosis != null) {
            diagnosisService.verify(diagnosis.id, "Current User")
        }
        return activeIncident()
    }

    @GET
    @Path("/home/stats")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
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
    @io.smallrye.common.annotation.Blocking
    fun recentIncidents(): TemplateInstance {
        val result = loadHomeData()
        val incidents = when (result) {
            is HomeResult.Success -> result.incidents
            is HomeResult.Failure -> emptyList()
        }
        return recentIncidentsTemplate.data("incidents", incidents)
    }

    @GET
    @Path("/home/incident-table")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun incidentTable(): TemplateInstance {
        val result = loadHomeData()
        val incidents = when (result) {
            is HomeResult.Success -> result.incidents
            is HomeResult.Failure -> emptyList()
        }
        return incidentTableTemplate.data("incidents", incidents)
    }

    @GET
    @Path("/home/active-incident")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun activeIncident(): TemplateInstance {
        val result = loadHomeData()
        val activeIncident = when (result) {
            is HomeResult.Success -> result.activeIncident
            is HomeResult.Failure -> null
        }
        return activeIncidentTemplate.data("activeIncident", activeIncident)
    }

    @GET
    @Path("/home/runbook-sidebar")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun runbookSidebar(): TemplateInstance {
        val result = loadHomeData()
        val runbookSteps = when (result) {
            is HomeResult.Success -> result.runbookSteps
            is HomeResult.Failure -> emptyList()
        }
        return runbookSidebarTemplate.data("steps", runbookSteps)
    }

    private fun loadHomeData(): HomeResult {
        return try {
            val allIncidents = incidentService.listRecent(limit = 100)
            
            val incidents = allIncidents.take(20)
            val diagnoses = diagnosisService.listRecent(limit = 50)

            val activeIncidents = incidents.count {
                it.status != com.example.incidentanalyst.incident.IncidentStatus.Resolved
            }

            val systemHealth = calculateSystemHealth(activeIncidents, incidents.size)
            val uptime = calculateUptime()

            val stats = StatsViewModel(
                totalIncidents = incidents.size,
                activeIncidents = activeIncidents,
                recentDiagnoses = diagnoses.size,
                meanResolutionTime = null,
                systemHealth = systemHealth,
                uptime = uptime
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
                    createdAt = incident.createdAt.toRelativeTime(),
                    shortDescription = incident.description.take(80),
                    tags = extractTags(incident.description, incident.source),
                    updatedAt = incident.updatedAt.toRelativeTime()
                )
            }

            val activeIncident = incidents.firstOrNull {
                it.status != com.example.incidentanalyst.incident.IncidentStatus.Resolved
            }?.let { incident ->
                val diagnosis = diagnosisService.getByIncidentId(incident.id.value)
                ActiveIncidentViewModel(
                    id = incident.id.value,
                    title = incident.title,
                    description = incident.description,
                    severity = incident.severity.name,
                    severityColor = incident.severity.toDaisyColor(),
                    status = incident.status.toDisplayString(),
                    statusColor = incident.status.toDaisyColor(),
                    createdAt = incident.createdAt.toRelativeTime(),
                    updatedAt = incident.updatedAt.toRelativeTime(),
                    source = incident.source,
                    hasDiagnosis = incident.status is com.example.incidentanalyst.incident.IncidentStatus.Diagnosed,
                    diagnosisProgress = generateDiagnosisProgress(incident.status),
                    verified = diagnosis?.verification is com.example.incidentanalyst.diagnosis.DiagnosisVerification.VerifiedByHuman
                )
            }

            val runbookSteps = generateRunbookSteps(activeIncident?.hasDiagnosis ?: false)

            HomeResult.Success(
                stats = stats,
                incidents = incidentCards,
                activeIncident = activeIncident,
                runbookSteps = runbookSteps
            )
        } catch (e: Exception) {
            HomeResult.Failure(HomeError.DataUnavailable)
        }
    }

    private fun calculateSystemHealth(activeIncidents: Int, totalIncidents: Int): Double {
        if (totalIncidents == 0) return 100.0
        val criticalCount = activeIncidents
        val healthScore = maxOf(0.0, 100.0 - (criticalCount * 10.0))
        return String.format("%.1f", healthScore).toDouble()
    }

    private fun calculateUptime(): String {
        val uptimeHours = 99.9
        return "$uptimeHours%"
    }

    private fun extractTags(description: String, source: String): List<String> {
        val tags = mutableListOf(source.uppercase())
        if (description.contains("latency", ignoreCase = true)) tags.add("LATENCY")
        if (description.contains("error", ignoreCase = true)) tags.add("ERROR")
        if (description.contains("timeout", ignoreCase = true)) tags.add("TIMEOUT")
        if (description.contains("memory", ignoreCase = true)) tags.add("MEMORY")
        if (description.contains("disk", ignoreCase = true)) tags.add("DISK")
        return tags.take(3)
    }

    private fun generateDiagnosisProgress(status: com.example.incidentanalyst.incident.IncidentStatus): List<DiagnosisStepViewModel> {
        return when (status) {
            is com.example.incidentanalyst.incident.IncidentStatus.Diagnosed -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "completed", "success"),
                DiagnosisStepViewModel(2, "Identifying patterns", "completed", "success"),
                DiagnosisStepViewModel(3, "Generating root cause", "completed", "success"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "completed", "success")
            )
            com.example.incidentanalyst.incident.IncidentStatus.Acknowledged -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "completed", "success"),
                DiagnosisStepViewModel(2, "Identifying patterns", "in-progress", "info"),
                DiagnosisStepViewModel(3, "Generating root cause", "pending", "neutral"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "pending", "neutral")
            )
            com.example.incidentanalyst.incident.IncidentStatus.Open -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "pending", "neutral"),
                DiagnosisStepViewModel(2, "Identifying patterns", "pending", "neutral"),
                DiagnosisStepViewModel(3, "Generating root cause", "pending", "neutral"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "pending", "neutral")
            )
            com.example.incidentanalyst.incident.IncidentStatus.Resolved -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "completed", "success"),
                DiagnosisStepViewModel(2, "Identifying patterns", "completed", "success"),
                DiagnosisStepViewModel(3, "Generating root cause", "completed", "success"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "completed", "success")
            )
        }
    }

    private fun generateRunbookSteps(hasDiagnosis: Boolean): List<RunbookStepViewModel> {
        return listOf(
            RunbookStepViewModel(1, "Acknowledge incident", "Verify incident details and acknowledge", true),
            RunbookStepViewModel(2, "Assess impact", "Evaluate service impact and severity", true),
            RunbookStepViewModel(3, "Review diagnosis", "Check AI-generated diagnosis and root cause", hasDiagnosis),
            RunbookStepViewModel(4, "Apply remediation", "Follow recommended remediation steps", !hasDiagnosis),
            RunbookStepViewModel(5, "Verify resolution", "Confirm system is operating normally", false)
        )
    }

    private fun createEmptyStats(): StatsViewModel = StatsViewModel(
        totalIncidents = 0,
        activeIncidents = 0,
        recentDiagnoses = 0,
        meanResolutionTime = null,
        systemHealth = 100.0,
        uptime = "99.9%"
    )
}
