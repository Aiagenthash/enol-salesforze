package com.enol.salesforze.data.remote.interceptors

import com.enol.salesforze.data.remote.session.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that injects the JWT Bearer token into outbound HTTP requests.
 * Reads seamlessly from SessionManager's in-memory cache with fallback to DataStore.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding Authorization header if already present (e.g. custom auth override)
        if (originalRequest.header("Authorization") != null) {
            return chain.proceed(originalRequest)
        }

        val token = sessionManager.getCachedAuthToken()
            ?: runBlocking { sessionManager.getAuthToken() }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
