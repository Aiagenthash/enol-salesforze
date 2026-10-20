package com.enol.salesforze.data.remote.exceptions

import java.io.IOException

/**
 * Base domain exception representing any REST API communication or protocol error.
 */
open class ApiException(
    val statusCode: Int,
    override val message: String,
    val errorBody: String? = null
) : IOException(message)

/**
 * Thrown on HTTP 401 Unauthorized, indicating JWT expired or credentials revoked.
 */
class UnauthorizedException(
    message: String = "Session expired or authentication failed. Please log in again.",
    errorBody: String? = null
) : ApiException(statusCode = 401, message = message, errorBody = errorBody)

/**
 * Thrown on HTTP 403 Forbidden, indicating lack of RBAC permissions.
 */
class ForbiddenException(
    message: String = "Access denied. You do not have permission to perform this action.",
    errorBody: String? = null
) : ApiException(statusCode = 403, message = message, errorBody = errorBody)

/**
 * Thrown on HTTP 404 Not Found.
 */
class NotFoundException(
    message: String = "Requested resource not found.",
    errorBody: String? = null
) : ApiException(statusCode = 404, message = message, errorBody = errorBody)

/**
 * Thrown on HTTP 409 Conflict (e.g. duplicate check-in, shift already active).
 */
class ConflictException(
    message: String = "Request conflicted with current server state.",
    errorBody: String? = null
) : ApiException(statusCode = 409, message = message, errorBody = errorBody)

/**
 * Thrown on HTTP 5xx Server Errors.
 */
class ServerException(
    statusCode: Int = 500,
    message: String = "Internal server error. Please try again later.",
    errorBody: String? = null
) : ApiException(statusCode = statusCode, message = message, errorBody = errorBody)
