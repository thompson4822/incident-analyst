package com.example.incidentanalyst.remediation

import com.example.incidentanalyst.diagnosis.DiagnosisService
import com.example.incidentanalyst.incident.IncidentId
import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Path("/incidents/{incidentId}/remediation")
class RemediationResource(
    private val remediationService: RemediationService,
    private val diagnosisService: DiagnosisService
) {

    @Inject
    @Location("remediation/progress-panel.html")
    lateinit var progressTemplate: Template

    @POST
    @Path("/start")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun startRemediation(@PathParam("incidentId") incidentId: Long): Response {
        val diagnosis = diagnosisService.getByIncidentId(incidentId)
            ?: return Response.status(Response.Status.BAD_REQUEST)
                .entity("No diagnosis found for incident")
                .build()

        // Start execution
        remediationService.executeAllSteps(IncidentId(incidentId), diagnosis)

        // Return the progress panel
        val progress = remediationService.getProgress(IncidentId(incidentId))
            ?: return Response.serverError().build()

        return Response.ok(progressTemplate.data("progress", progress)).build()
    }

    @GET
    @Path("/progress")
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    fun getProgress(@PathParam("incidentId") incidentId: Long): Response {
        val progress = remediationService.getProgress(IncidentId(incidentId))
            ?: return Response.ok("<div class=\"text-slate-500\">No remediation in progress</div>").build()

        return Response.ok(progressTemplate.data("progress", progress)).build()
    }
}
