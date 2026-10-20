package com.enol.salesforze.data.remote.interceptors

import com.enol.salesforze.data.remote.session.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that injects the X-API-Key header into outbound HTTP requests.
 * Reads the configured API Key from SessionManager or runtime configuration.
 */
@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding X-API-Key header if already present
        if (originalRequest.header("X-API-Key") != null) {
            return chain.proceed(originalRequest)
        }

        val apiKey = sessionManager.getCachedApiKey()
            ?: runBlocking { sessionManager.getApiKey() }

        val requestBuilder = originalRequest.newBuilder()
        if (!apiKey.isNullOrBlank()) {
            requestBuilder.header("X-API-Key", apiKey)
        }

        return chain.proceed(requestBuilder.build())
    }
}
