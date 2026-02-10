package com.example.incidentanalyst.home

import com.example.incidentanalyst.agent.IncidentDiagnosisService
import com.example.incidentanalyst.diagnosis.DiagnosisRepository
import com.example.incidentanalyst.diagnosis.DiagnosisService
import com.example.incidentanalyst.incident.IncidentId
import com.example.incidentanalyst.incident.IncidentRepository
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.web.toDaisyColor
import com.example.incidentanalyst.web.toDisplayString
import com.example.incidentanalyst.web.toRelativeTime
import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
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

    @GET
    @Path("/home/stats")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun stats(): TemplateInstance {
        val incidents = incidentService.listRecent()
        val total = incidents.size
        val active = incidents.count { it.status != com.example.incidentanalyst.incident.IncidentStatus.Resolved }
        val diagnosed = incidents.count { it.status is com.example.incidentanalyst.incident.IncidentStatus.Diagnosed }
        
        val model = DashboardStatsViewModel(
            systemHealth = 70.0,
            uptime = 99.9,
            totalIncidents = total,
            activeIncidents = active,
            diagnosedIncidents = diagnosed,
            avgResolutionTime = "--"
        )
        return statsTemplate.data("stats", model)
    }

    @GET
    @Path("/home/recent-incidents")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun recentIncidents(): TemplateInstance {
        val incidents = incidentService.listRecent(limit = 5)
        val items = incidents.map { it.toHomeItemViewModel() }
        return recentIncidentsTemplate.data("incidents", items)
    }

    @GET
    @Path("/home/incident-table")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun incidentTable(): TemplateInstance {
        val incidents = incidentService.listRecent(limit = 10)
        val items = incidents.map { it.toHomeItemViewModel() }
        return incidentTableTemplate.data("incidents", items)
    }

    @GET
    @Path("/home/active-incident")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun activeIncident(): TemplateInstance {
        val incidents = incidentService.listRecent()
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
        return activeIncidentTemplate.data("activeIncident", activeIncident)
    }

    @GET
    @Path("/home/runbook-sidebar")
    @Produces(MediaType.TEXT_HTML)
    @io.smallrye.common.annotation.Blocking
    fun runbookSidebar(): TemplateInstance {
        val steps = listOf(
            ResponseStepViewModel("Acknowledge incident", "Verify incident details and acknowledge", "completed", "success"),
            ResponseStepViewModel("Assess impact", "Evaluate service impact and severity", "completed", "success"),
            ResponseStepViewModel("Review diagnosis", "Check AI-generated diagnosis and root cause", "pending", "neutral"),
            ResponseStepViewModel("Apply remediation", "Follow recommended remediation steps", "completed", "success"),
            ResponseStepViewModel("Verify resolution", "Confirm system is operating normally", "pending", "neutral")
        )
        val model = ResponsePlanViewModel("Standard", steps)
        return runbookSidebarTemplate.data("plan", model)
    }

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
        val diagnosis = diagnosisService.getByIncidentId(id)
        if (diagnosis != null) {
            diagnosisService.verify(diagnosis.id, "Current User")
        }
        return activeIncident()
    }

    private fun com.example.incidentanalyst.incident.Incident.toHomeItemViewModel() = HomeIncidentItemViewModel(
        id = id.value,
        source = source,
        severity = severity.name,
        severityColor = severity.toDaisyColor(),
        title = title,
        description = description,
        status = status.toDisplayString(),
        statusColor = status.toDaisyColor(),
        updatedAt = updatedAt.toRelativeTime(),
        tags = listOf(source.uppercase(), if (description.contains("memory", true)) "MEMORY" else "ERROR")
    )

    private fun generateDiagnosisProgress(status: com.example.incidentanalyst.incident.IncidentStatus): List<DiagnosisStepViewModel> {
        return when (status) {
            is com.example.incidentanalyst.incident.IncidentStatus.Diagnosed -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "completed", "success"),
                DiagnosisStepViewModel(2, "Identifying patterns", "completed", "success"),
                DiagnosisStepViewModel(3, "Generating root cause", "completed", "success"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "completed", "success")
            )
            else -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "pending", "neutral"),
                DiagnosisStepViewModel(2, "Identifying patterns", "pending", "neutral"),
                DiagnosisStepViewModel(3, "Generating root cause", "pending", "neutral"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "pending", "neutral")
            )
        }
    }
}
