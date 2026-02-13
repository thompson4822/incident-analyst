package com.example.incidentanalyst.training

import com.example.incidentanalyst.incident.Incident
import com.example.incidentanalyst.incident.IncidentService
import com.example.incidentanalyst.incident.IncidentStatus
import com.example.incidentanalyst.incident.IncidentId
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path as JaxRsPath
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@JaxRsPath("/training")
class TrainingIncidentResource @Inject constructor(
    private val incidentService: IncidentService,
    private val objectMapper: ObjectMapper,
    @ConfigProperty(name = "training.log-directory", defaultValue = "logs")
    logDirectory: String
) {

    private val log = Logger.getLogger(javaClass)
    private val logDirectoryPath: Path = java.nio.file.Paths.get(logDirectory)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @POST
    @JaxRsPath("/incidents")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createTrainingIncident(@Valid request: TrainingIncidentRequestDto): Response {
        val timestamp = request.timestamp ?: Instant.now()

        val incident = Incident(
            id = IncidentId(0),
            source = request.source,
            title = request.title,
            description = buildDescription(request.description, request.stackTrace),
            severity = request.severity,
            status = IncidentStatus.Open,
            createdAt = timestamp,
            updatedAt = timestamp
        )

        val createdIncident = incidentService.create(incident)

        logTrainingData(request, createdIncident)

        return Response.ok(toTrainingIncidentResponseDto(createdIncident)).build()
    }

    private fun buildDescription(description: String, stackTrace: String?): String {
        return if (stackTrace.isNullOrBlank()) {
            description
        } else {
            """$description

Stack Trace:
$stackTrace""".trimIndent()
        }
    }

    private fun logTrainingData(request: TrainingIncidentRequestDto, createdIncident: Incident) {
        try {
            Files.createDirectories(logDirectoryPath)

            val fileName = "training-incidents-${LocalDate.now().format(dateFormatter)}.jsonl"
            val logFile = logDirectoryPath.resolve(fileName)

            val logEntry = objectMapper.writeValueAsString(mapOf(
                "title" to request.title,
                "description" to request.description,
                "severity" to request.severity.name,
                "timestamp" to (request.timestamp?.toString() ?: createdIncident.createdAt.toString()),
                "stackTrace" to request.stackTrace,
                "source" to request.source,
                "createdAt" to createdIncident.createdAt.toString()
            ))

            Files.writeString(
                logFile,
                "$logEntry\n",
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
            )

            log.infof("Logged training data to %s", logFile)
        } catch (e: Exception) {
            log.warnf(e, "Failed to log training data to file")
        }
    }
}
