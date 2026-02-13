package com.example.incidentanalyst.common

import jakarta.validation.ConstraintViolationException
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

/**
 * Maps ConstraintViolationException to a consistent JSON error response.
 * This allows resources to use @Valid for automatic validation while
 * maintaining a consistent error response format.
 */
@Provider
class ValidationExceptionMapper : ExceptionMapper<ConstraintViolationException> {
    
    override fun toResponse(exception: ConstraintViolationException): Response {
        val errors = exception.constraintViolations.map { violation ->
            "${violation.propertyPath} ${violation.message}"
        }
        
        return Response.status(Response.Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(ValidationErrorResponse(
                message = "Validation failed",
                errors = errors
            ))
            .build()
    }
}

data class ValidationErrorResponse(
    val message: String,
    val errors: List<String>
)
