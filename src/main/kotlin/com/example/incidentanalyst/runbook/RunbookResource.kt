package com.example.incidentanalyst.runbook

import com.example.incidentanalyst.web.toRelativeTime
import io.quarkus.qute.Location
import io.quarkus.qute.Template
import io.quarkus.qute.TemplateInstance
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import java.time.Instant

@Path("/runbooks")
class RunbookResource(
    private val runbookService: RunbookService
) {

    @Inject
    @Location("runbook/runbook-list.html")
    lateinit var listTemplate: Template

    @Inject
    @Location("runbook/runbook-edit.html")
    lateinit var editTemplate: Template

    @GET
    @Produces(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
    @Blocking
    fun list(
        @Context headers: HttpHeaders,
        @QueryParam("q") query: String?,
        @QueryParam("category") category: String?
    ): Response {
        val runbooks = runbookService.search(query, category)
        
        // Check if client prefers HTML
        val acceptHeader = headers.acceptableMediaTypes
        if (acceptHeader.any { it.isCompatible(MediaType.TEXT_HTML_TYPE) }) {
            val viewModel = mapToListViewModel(runbooks, query, category)
            return Response.ok(listTemplate.data("model", viewModel))
                .type(MediaType.TEXT_HTML)
                .build()
        }
        
        return Response.ok(runbooks.map { it.toResponseDto() })
            .type(MediaType.APPLICATION_JSON)
            .build()
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.TEXT_HTML, MediaType.APPLICATION_JSON)
    @Blocking
    fun getById(@PathParam("id") id: Long, @Context headers: HttpHeaders): Response {
        val fragmentId = RunbookFragmentId(id)
        return runbookService.getById(fragmentId).fold(
            ifLeft = { Response.status(Response.Status.NOT_FOUND).build() },
            ifRight = { fragment ->
                // Check if client prefers HTML
                val acceptHeader = headers.acceptableMediaTypes
                if (acceptHeader.any { it.isCompatible(MediaType.TEXT_HTML_TYPE) }) {
                    val viewModel = mapToDetailViewModel(fragment)
                    Response.ok(editTemplate.data("runbook", viewModel))
                        .type(MediaType.TEXT_HTML)
                        .build()
                } else {
                    Response.ok(fragment.toResponseDto())
                        .type(MediaType.APPLICATION_JSON)
                        .build()
                }
            }
        )
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun createFragment(@Valid request: RunbookFragmentCreateRequestDto): Response {
        val fragment = runbookService.createFragment(request.title, request.content, request.tags)
        return Response.status(Response.Status.CREATED).entity(fragment.toResponseDto()).build()
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    fun updateFragment(
        @PathParam("id") id: Long,
        @Valid request: RunbookFragmentUpdateRequestDto
    ): Response {
        val fragmentId = RunbookFragmentId(id)
        return runbookService.updateFragment(fragmentId, request.title, request.content, request.tags).fold(
            ifLeft = { error ->
                when (error) {
                    is RunbookFragmentError.NotFound -> Response.status(Response.Status.NOT_FOUND).build()
                    is RunbookFragmentError.ValidationFailed -> Response.status(Response.Status.BAD_REQUEST)
                        .type(MediaType.APPLICATION_JSON)
                        .entity(mapOf("error" to "Invalid request data"))
                        .build()
                }
            },
            ifRight = { fragment ->
                Response.ok(fragment.toResponseDto()).build()
            }
        )
    }

    private fun mapToListViewModel(
        runbooks: List<RunbookFragment>,
        query: String? = null,
        category: String? = null
    ): RunbookListViewModel {
        val items = runbooks.map { fragment ->
            RunbookListItemViewModel(
                id = fragment.id.value,
                title = fragment.title,
                shortContent = fragment.content.take(100),
                tags = fragment.tags?.split(",")?.map { it.trim() } ?: emptyList(),
                createdAt = fragment.createdAt.toRelativeTime(),
                stepCount = fragment.content.split("\n").count { it.startsWith("-") || it.startsWith("*") || it.matches(Regex("^\\d+\\..*")) },
                version = "1.0",
                severity = "Medium",
                severityColor = "info"
            )
        }

        return RunbookListViewModel(
            runbooks = items,
            totalCount = runbooks.size,
            categories = listOf("Network", "Database", "Application", "Security"),
            filters = RunbookFiltersViewModel(query, category)
        )
    }

    private fun mapToDetailViewModel(fragment: RunbookFragment): RunbookDetailViewModel {
        val steps = fragment.content.split("\n")
            .filter { it.isNotBlank() }
            .mapIndexed { index, line ->
                RunbookStepViewModel(
                    order = index + 1,
                    title = line.replace(Regex("^[-*\\d.]+\\s*"), "").take(50),
                    description = line.replace(Regex("^[-*\\d.]+\\s*"), ""),
                    completed = false
                )
            }

        return RunbookDetailViewModel(
            id = fragment.id.value,
            title = fragment.title,
            content = fragment.content,
            tags = fragment.tags?.split(",")?.map { it.trim() } ?: emptyList(),
            createdAt = fragment.createdAt.toRelativeTime(),
            version = "1.0",
            severity = "Medium",
            severityColor = "info",
            category = "General",
            steps = steps
        )
    }
}
