package com.example.incidentanalyst.common

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validator

/**
 * Extension function to validate an object and return a list of error messages.
 * Returns an empty list if validation passes.
 */
fun <T> Validator.validateToList(obj: T): List<String> =
    this.validate(obj).map { violation ->
        "${violation.propertyPath} ${violation.message}"
    }

/**
 * Validate an object and return Either with the validation errors on the left.
 * Useful for integrating with the Either pattern in services.
 */
inline fun <T, reified E> Validator.validateEither(obj: T, errorFactory: (List<String>) -> E): Either<E, T> {
    val errors = this.validateToList(obj)
    return if (errors.isEmpty()) {
        Either.Right(obj)
    } else {
        Either.Left(errorFactory(errors))
    }
}
