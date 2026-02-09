package com.example.incidentanalyst.incident

import com.example.incidentanalyst.diagnosis.DiagnosisService
import com.example.incidentanalyst.home.DiagnosisStepViewModel
import com.example.incidentanalyst.web.toDaisyColor
import com.example.incidentanalyst.web.toDisplayString
import com.example.incidentanalyst.web.toRelativeTime
import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Duration
import java.time.Instant

@Path("/incidents")
class IncidentResource(
    private val incidentService: IncidentService,
    private val diagnosisService: DiagnosisService
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
    fun list(@Context headers: HttpHeaders): Response {
        val incidents = incidentService.listRecent()
        
        // Check if client prefers HTML
        val acceptHeader = headers.acceptableMediaTypes
        if (acceptHeader.any { it.isCompatible(MediaType.TEXT_HTML_TYPE) }) {
            val viewModel = mapToListViewModel(incidents)
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
        // Validate ID - reject negative or zero values
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Incident ID must be a positive number"))
                .build()
        }
        
        val incidentId = IncidentId(id)
        val result = incidentService.getById(incidentId)
        
        if (result is IncidentResult.Failure) {
            return Response.status(Response.Status.NOT_FOUND).build()
        }
        
        val incident = (result as IncidentResult.Success).incident
        
        // Check if client prefers HTML
        val acceptHeader = headers.acceptableMediaTypes
        if (acceptHeader.any { it.isCompatible(MediaType.TEXT_HTML_TYPE) }) {
            val viewModel = mapToDetailViewModel(incident)
            return Response.ok(detailTemplate.data("incident", viewModel))
                .type(MediaType.TEXT_HTML)
                .build()
        }
        
        return Response.ok(incident.toResponseDto())
            .type(MediaType.APPLICATION_JSON)
            .build()
    }
        
        return Response.ok(incidents.map { it.toResponseDto() }).build()
    }

    private fun mapToListViewModel(incidents: List<Incident>): IncidentListViewModel {
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
            filters = IncidentFiltersViewModel()
        )
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
    @Blocking
    fun getById(@PathParam("id") id: Long, @Context headers: HttpHeaders): Response {
        // Validate ID - reject negative or zero values
        if (id <= 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Incident ID must be a positive number"))
                .build()
        }
        
        val incidentId = IncidentId(id)
        val result = incidentService.getById(incidentId)
        
        if (result is IncidentResult.Failure) {
            return Response.status(Response.Status.NOT_FOUND).build()
        }
        
        val incident = (result as IncidentResult.Success).incident
        
        // Check if client prefers HTML
        val acceptHeader = headers.acceptableMediaTypes
        if (acceptHeader.any { it.isCompatible(MediaType.TEXT_HTML_TYPE) }) {
            val viewModel = mapToDetailViewModel(incident)
            return Response.ok(detailTemplate.data("incident", viewModel)).build()
        }
        
        return Response.ok(incident.toResponseDto()).build()
    }

    @POST
    @Path("/{id}/acknowledge")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun acknowledge(@PathParam("id") id: Long, @Context headers: HttpHeaders): Response {
        val incidentId = IncidentId(id)
        return when (val result = incidentService.updateStatus(incidentId, IncidentStatus.Acknowledged)) {
            is IncidentResult.Success -> {
                val viewModel = mapToDetailViewModel(result.incident)
                val isHtmx = headers.getHeaderString("HX-Request") != null
                if (isHtmx) {
                    // Return only the fragment if possible, or just the full page for now but with a different target
                    // For now, let's just return the full page and fix the target in HTML
                    Response.ok(detailTemplate.data("incident", viewModel)).build()
                } else {
                    Response.ok(detailTemplate.data("incident", viewModel)).build()
                }
            }
            is IncidentResult.Failure -> Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @POST
    @Path("/{id}/resolve")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun resolve(@PathParam("id") id: Long, @Context headers: HttpHeaders): Response {
        val incidentId = IncidentId(id)
        return when (val result = incidentService.updateStatus(incidentId, IncidentStatus.Resolved)) {
            is IncidentResult.Success -> {
                val viewModel = mapToDetailViewModel(result.incident)
                Response.ok(detailTemplate.data("incident", viewModel)).build()
            }
            is IncidentResult.Failure -> Response.status(Response.Status.NOT_FOUND).build()
        }
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
