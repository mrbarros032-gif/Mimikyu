package com.lin0721.linmusic.core.network

import com.lin0721.linmusic.core.log.AppLogger
import okhttp3.Interceptor
import okhttp3.Response

private const val TAG = "EmptyBodyInterceptor"

// ：。 kotlinx.serialization  EOF 。
class EmptyBodyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val bodyString = response.peekBody(Long.MAX_VALUE).string()
        if (bodyString.isBlank()) {
            AppLogger.w(TAG, "Empty response body, possibly restricted/logged out: ${request.url} code=${response.code}")
            throw ApiException("API body is empty, possibly auth failed")
        }
        
        return response
    }
}
