package com.lin0721.linmusic.core.network

// Repository  of ，UI ， of
sealed class AppError : Exception() {
    // （IOException ）， ApiException
    data object NetworkError : AppError()

    // ApiException（0 ，/）
    data object RiskControl : AppError()

    // code == 301， of
    data object Unauthorized : AppError()

    // /
    data object ParseError : AppError()

    // of
    data class BizError(val code: Int, val rawMsg: String?) : AppError()
}
