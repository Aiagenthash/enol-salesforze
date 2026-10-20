package com.enol.salesforze.data.remote.interceptors

import com.enol.salesforze.data.remote.exceptions.ApiException
import com.enol.salesforze.data.remote.exceptions.ConflictException
import com.enol.salesforze.data.remote.exceptions.ForbiddenException
import com.enol.salesforze.data.remote.exceptions.NotFoundException
import com.enol.salesforze.data.remote.exceptions.ServerException
import com.enol.salesforze.data.remote.exceptions.UnauthorizedException
import com.enol.salesforze.data.remote.session.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Intercepts HTTP errors (401, 403, 404, 409, 5xx), parses error payloads,
 * triggers session revocation on 401, and maps responses to custom domain exceptions.
 */
@Singleton
class ErrorInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.isSuccessful) {
            return response
        }

        val statusCode = response.code
        val rawBody = try {
            response.peekBody(1024 * 64).string()
        } catch (_: Exception) {
            null
        }

        val parsedErrorMessage = extractErrorMessage(rawBody, response.message)

        when (statusCode) {
            401 -> {
                // Signal session expiry and clear persisted session asynchronously
                sessionManager.notifySessionExpired()
                CoroutineScope(Dispatchers.IO).launch {
                    sessionManager.clearSession()
                }
                throw UnauthorizedException(
                    message = parsedErrorMessage.ifBlank { "Session expired or unauthorized. Please log in again." },
                    errorBody = rawBody
                )
            }
            403 -> {
                throw ForbiddenException(
                    message = parsedErrorMessage.ifBlank { "Access forbidden. You do not have permissions." },
                    errorBody = rawBody
                )
            }
            404 -> {
                throw NotFoundException(
                    message = parsedErrorMessage.ifBlank { "Resource not found." },
                    errorBody = rawBody
                )
            }
            409 -> {
                throw ConflictException(
                    message = parsedErrorMessage.ifBlank { "Resource conflict occurred." },
                    errorBody = rawBody
                )
            }
            in 500..599 -> {
                throw ServerException(
                    statusCode = statusCode,
                    message = parsedErrorMessage.ifBlank { "Internal server error ($statusCode). Please try again." },
                    errorBody = rawBody
                )
            }
            else -> {
                throw ApiException(
                    statusCode = statusCode,
                    message = parsedErrorMessage.ifBlank { "HTTP Error $statusCode: ${response.message}" },
                    errorBody = rawBody
                )
            }
        }
    }

    private fun extractErrorMessage(rawBody: String?, fallback: String): String {
        if (rawBody.isNullOrBlank()) return fallback
        return try {
            val json = JSONObject(rawBody)
            when {
                json.has("error") && !json.isNull("error") -> json.getString("error")
                json.has("message") && !json.isNull("message") -> json.getString("message")
                else -> fallback
            }
        } catch (_: Exception) {
            fallback
        }
    }
}
