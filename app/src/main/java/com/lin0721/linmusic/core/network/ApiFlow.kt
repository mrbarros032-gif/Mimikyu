package com.lin0721.linmusic.core.network

import com.lin0721.linmusic.core.log.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import java.io.IOException

/**
 *  Repository  of  flow + catch + ， of  flow{}.catch{} 。
 *  DTO  of /， lambda 。
 */
inline fun <T, R> apiFlow(
    crossinline request: suspend () -> T,
    crossinline isSuccess: (T) -> Boolean,
    crossinline code: (T) -> Int,
    crossinline msg: (T) -> String? = { null },
    crossinline transform: suspend (T) -> R
): Flow<Result<R>> = flow {
    val response = request()
    if (isSuccess(response)) {
        emit(Result.success(transform(response)))
    } else if (code(response) == 301) {
        AppLogger.w("ApiFlow", "Endpoint returned 301 (Unauthorized): ${msg(response) ?: "requires login"}")
        emit(Result.failure(AppError.Unauthorized))
    } else {
        AppLogger.e("ApiFlow", "Business error code: code=${code(response)} msg=${msg(response)}")
        emit(Result.failure(AppError.BizError(code(response), msg(response))))
    }
}.catch { e ->
    AppLogger.e("ApiFlow", "Request error: ${e::class.simpleName}", e)
    emit(Result.failure(mapToAppError(e)))
}

// / AppError
fun mapToAppError(e: Throwable): Throwable = when (e) {
    is AppError -> e
    is ApiException -> AppError.RiskControl
    is SerializationException -> AppError.ParseError
    is IOException -> AppError.NetworkError
    else -> e
}
