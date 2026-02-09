package com.example.incidentanalyst.incident

import com.example.incidentanalyst.agent.IncidentDiagnosisService
import com.example.incidentanalyst.diagnosis.DiagnosisResult
import com.example.incidentanalyst.diagnosis.DiagnosisService
import com.example.incidentanalyst.home.DiagnosisStepViewModel
import com.example.incidentanalyst.web.toDaisyColor
import com.example.incidentanalyst.web.toDisplayString
import com.example.incidentanalyst.web.toRelativeTime
import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Duration
import java.time.Instant

@Path("/incidents")
class IncidentResource(
    private val incidentService: IncidentService,
    private val diagnosisService: DiagnosisService,
    private val incidentDiagnosisService: IncidentDiagnosisService
) {

    @Inject
    @Location("incident/detail.html")
    lateinit var detailTemplate: Template

    @Inject
    @Location("incident/list.html")
    lateinit var listTemplate: Template

    @GET
    @Produces(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
    @Blocking
    fun list(
        @Context headers: HttpHeaders,
        @QueryParam("q") query: String?,
        @QueryParam("status") status: String?,
        @QueryParam("severity") severity: String?,
        @QueryParam("source") source: String?
    ): Response {
        val incidents = incidentService.search(query, status, severity, source)
        
        // Check if client prefers HTML
        val acceptHeader = headers.acceptableMediaTypes
        if (acceptHeader.any { it.isCompatible(MediaType.TEXT_HTML_TYPE) }) {
            val viewModel = mapToListViewModel(incidents, query, status, severity, source)
            return Response.ok(listTemplate.data("model", viewModel))
                .type(MediaType.TEXT_HTML)
                .build()
        }
        
        return Response.ok(incidents.map { it.toResponseDto() })
            .type(MediaType.APPLICATION_JSON)
            .build()
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
    @Blocking
    fun getById(@PathParam("id") id: Long, @Context headers: HttpHeaders): Response {
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Incident ID must be a positive number"))
                .build()
        }
        
        val incidentId = IncidentId(id)
        val result = incidentService.getById(incidentId)
        
        return result.fold(
            ifLeft = { Response.status(Response.Status.NOT_FOUND).build() },
            ifRight = { incident ->
                val acceptHeader = headers.acceptableMediaTypes
                if (acceptHeader.any { it.isCompatible(MediaType.TEXT_HTML_TYPE) }) {
                    val viewModel = mapToDetailViewModel(incident)
                    Response.ok(detailTemplate.data("incident", viewModel))
                        .type(MediaType.TEXT_HTML)
                        .build()
                } else {
                    Response.ok(incident.toResponseDto())
                        .type(MediaType.APPLICATION_JSON)
                        .build()
                }
            }
        )
    }

    @POST
    @Path("/{id}/acknowledge")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun acknowledge(@PathParam("id") id: Long, @Context headers: HttpHeaders): Response {
        val incidentId = IncidentId(id)
        return incidentService.updateStatus(incidentId, IncidentStatus.Acknowledged).fold(
            ifLeft = { Response.status(Response.Status.NOT_FOUND).build() },
            ifRight = { incident ->
                val viewModel = mapToDetailViewModel(incident)
                Response.ok(detailTemplate.data("incident", viewModel)).build()
            }
        )
    }

    @POST
    @Path("/{id}/resolve")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun resolve(@PathParam("id") id: Long, @Context headers: HttpHeaders): Response {
        val incidentId = IncidentId(id)
        return incidentService.updateStatus(incidentId, IncidentStatus.Resolved).fold(
            ifLeft = { Response.status(Response.Status.NOT_FOUND).build() },
            ifRight = { incident ->
                val viewModel = mapToDetailViewModel(incident)
                Response.ok(detailTemplate.data("incident", viewModel)).build()
            }
        )
    }

    @POST
    @Path("/{id}/diagnose")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun diagnose(@PathParam("id") id: Long): Response {
        val incidentId = IncidentId(id)
        return incidentDiagnosisService.diagnose(incidentId).fold(
            ifLeft = { error ->
                // Return the detail view anyway, maybe with an error message in the future
                incidentService.getById(incidentId).fold(
                    ifLeft = { Response.status(Response.Status.NOT_FOUND).build() },
                    ifRight = { incident ->
                        val viewModel = mapToDetailViewModel(incident)
                        Response.ok(detailTemplate.data("incident", viewModel)).build()
                    }
                )
            },
            ifRight = { success ->
                // After diagnosis, we want to refresh the incident detail view
                incidentService.getById(incidentId).fold(
                    ifLeft = { Response.status(Response.Status.INTERNAL_SERVER_ERROR).build() },
                    ifRight = { incident ->
                        val viewModel = mapToDetailViewModel(incident)
                        // We could add a "New" flag to the view model here if we wanted
                        Response.ok(detailTemplate.data("incident", viewModel)).build()
                    }
                )
            }
        )
    }

    @POST
    @Path("/{id}/remediate")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun remediate(@PathParam("id") id: Long): Response {
        val incidentId = IncidentId(id)
        // In a real app, this would trigger actual remediation actions
        // For now, we'll just resolve the incident
        return incidentService.updateStatus(incidentId, IncidentStatus.Resolved).fold(
            ifLeft = { Response.status(Response.Status.NOT_FOUND).build() },
            ifRight = { incident ->
                val viewModel = mapToDetailViewModel(incident)
                Response.ok(detailTemplate.data("incident", viewModel)).build()
            }
        )
    }

    private fun mapToListViewModel(
        incidents: List<Incident>,
        query: String?,
        status: String?,
        severity: String?,
        source: String?
    ): IncidentListViewModel {
        val activeCount = incidents.count { it.status != IncidentStatus.Resolved }
        
        val items = incidents.map { incident ->
            IncidentListItemViewModel(
                id = incident.id.value,
                title = incident.title,
                shortDescription = incident.description.take(100),
                source = incident.source,
                severity = incident.severity.name,
                severityColor = incident.severity.toDaisyColor(),
                status = incident.status.toDisplayString(),
                statusColor = incident.status.toDaisyColor(),
                createdAt = incident.createdAt.toRelativeTime(),
                updatedAt = incident.updatedAt.toRelativeTime(),
                tags = extractTags(incident.description, incident.source)
            )
        }

        return IncidentListViewModel(
            incidents = items,
            totalCount = incidents.size,
            activeCount = activeCount,
            filters = IncidentFiltersViewModel(
                query = query,
                severity = severity,
                status = status,
                source = source
            )
        )
    }

    private fun mapToDetailViewModel(incident: Incident): IncidentDetailViewModel {
        val diagnosis = diagnosisService.getByIncidentId(incident.id.value)
        
        val duration = Duration.between(incident.createdAt, Instant.now())
        val durationStr = when {
            duration.toDays() > 0 -> "${duration.toDays()}d ${duration.toHoursPart()}h"
            duration.toHours() > 0 -> "${duration.toHours()}h ${duration.toMinutesPart()}m"
            else -> "${duration.toMinutes()}m"
        }

        return IncidentDetailViewModel(
            id = incident.id.value,
            title = incident.title,
            description = incident.description,
            source = incident.source,
            severity = incident.severity.name,
            severityColor = incident.severity.toDaisyColor(),
            status = incident.status.toDisplayString(),
            statusColor = incident.status.toDaisyColor(),
            createdAt = incident.createdAt.toRelativeTime(),
            updatedAt = incident.updatedAt.toRelativeTime(),
            duration = durationStr,
            tags = extractTags(incident.description, incident.source),
            assignee = "Unassigned",
            diagnosis = diagnosis?.let { d ->
                DiagnosisViewModel(
                    id = d.id.value,
                    rootCause = d.rootCause,
                    steps = d.steps,
                    confidence = d.confidence.name,
                    confidenceColor = when (d.confidence.name) {
                        "HIGH" -> "success"
                        "MEDIUM" -> "warning"
                        else -> "error"
                    },
                    progress = generateDiagnosisProgress(incident.status)
                )
            },
            timeline = generateTimeline(incident)
        )
    }

    private fun extractTags(description: String, source: String): List<String> {
        val tags = mutableListOf(source.uppercase())
        if (description.contains("latency", ignoreCase = true)) tags.add("LATENCY")
        if (description.contains("error", ignoreCase = true)) tags.add("ERROR")
        if (description.contains("timeout", ignoreCase = true)) tags.add("TIMEOUT")
        if (description.contains("memory", ignoreCase = true)) tags.add("MEMORY")
        if (description.contains("disk", ignoreCase = true)) tags.add("DISK")
        return tags.take(5)
    }

    private fun generateDiagnosisProgress(status: IncidentStatus): List<DiagnosisStepViewModel> {
        return when (status) {
            is IncidentStatus.Diagnosed -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "completed", "success"),
                DiagnosisStepViewModel(2, "Identifying patterns", "completed", "success"),
                DiagnosisStepViewModel(3, "Generating root cause", "completed", "success"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "completed", "success")
            )
            IncidentStatus.Acknowledged -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "completed", "success"),
                DiagnosisStepViewModel(2, "Identifying patterns", "in-progress", "info"),
                DiagnosisStepViewModel(3, "Generating root cause", "pending", "neutral"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "pending", "neutral")
            )
            IncidentStatus.Open -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "pending", "neutral"),
                DiagnosisStepViewModel(2, "Identifying patterns", "pending", "neutral"),
                DiagnosisStepViewModel(3, "Generating root cause", "pending", "neutral"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "pending", "neutral")
            )
            IncidentStatus.Resolved -> listOf(
                DiagnosisStepViewModel(1, "Analyzing logs", "completed", "success"),
                DiagnosisStepViewModel(2, "Identifying patterns", "completed", "success"),
                DiagnosisStepViewModel(3, "Generating root cause", "completed", "success"),
                DiagnosisStepViewModel(4, "Creating remediation plan", "completed", "success")
            )
        }
    }

    private fun generateTimeline(incident: Incident): List<TimelineEventViewModel> {
        val events = mutableListOf<TimelineEventViewModel>()
        
        events.add(TimelineEventViewModel(
            timestamp = incident.createdAt.toRelativeTime(),
            action = "Incident Created",
            description = "Detected via ${incident.source}",
            color = "error",
            icon = "plus"
        ))
        
        if (incident.status != IncidentStatus.Open) {
            events.add(TimelineEventViewModel(
                timestamp = incident.updatedAt.toRelativeTime(),
                action = "Status Updated",
                description = "Changed to ${incident.status.toDisplayString()}",
                color = "info",
                icon = "check"
            ))
        }
        
        return events.reversed()
    }
}
